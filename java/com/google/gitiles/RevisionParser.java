// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gitiles;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.hash;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;

/** Object to parse revisions out of Gitiles paths. */
class RevisionParser {
  private static final Splitter OPERATOR_SPLITTER = Splitter.on(CharMatcher.anyOf("^~"));

  // The ref name part of a revision expression ends at the first
  // appearance of ^, ~, :, or @{ (see git-check-ref-format(1)).
  private static final Pattern END_OF_REF = Pattern.compile("[\\^~:]|@\\{");

  static class Result {
    private final Revision revision;
    private final Revision oldRevision;
    private final String path;

    @VisibleForTesting
    Result(Revision revision) {
      this(revision, null, "");
    }

    @VisibleForTesting
    Result(Revision revision, Revision oldRevision, String path) {
      this.revision = revision;
      this.oldRevision = oldRevision;
      this.path = path;
    }

    public Revision getRevision() {
      return revision;
    }

    public Revision getOldRevision() {
      return oldRevision;
    }

    public String getPath() {
      return path;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Result) {
        Result r = (Result) o;
        return Objects.equals(revision, r.revision)
            && Objects.equals(oldRevision, r.oldRevision)
            && Objects.equals(path, r.path);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return hash(revision, oldRevision, path);
    }

    @Override
    public String toString() {
      return toStringHelper(this)
          .omitNullValues()
          .add("revision", revision.getName())
          .add("oldRevision", oldRevision != null ? oldRevision.getName() : null)
          .add("path", path)
          .toString();
    }
  }

  private final Repository repo;
  private final GitilesAccess access;
  private final VisibilityCache cache;
  private final BranchRedirect branchRedirect;

  RevisionParser(
      Repository repo, GitilesAccess access, VisibilityCache cache, BranchRedirect branchRedirect) {
    this.repo = checkNotNull(repo, "repo");
    this.access = checkNotNull(access, "access");
    this.cache = checkNotNull(cache, "cache");
    this.branchRedirect = checkNotNull(branchRedirect, "branchRedirect");
  }

  @Nullable
  Result parse(String path) throws IOException {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    if (Strings.isNullOrEmpty(path)) {
      return null;
    }
    try (RevWalk walk = new RevWalk(repo)) {
      walk.setRetainBody(false);

      Revision oldRevision = null;
      Revision oldRevisionRedirected = null;

      StringBuilder b = new StringBuilder();
      boolean first = true;
      for (String part : PathUtil.SPLITTER.split(path)) {
        if (part.isEmpty()) {
          return null; // No valid revision contains empty segments.
        }
        if (!first) {
          b.append('/');
        }

        if (oldRevision == null) {
          int dots = part.indexOf("..");
          int firstParent = part.indexOf("^!");
          if (dots == 0 || firstParent == 0) {
            return null;
          } else if (dots > 0) {
            b.append(part, 0, dots);
            String oldName = b.toString();
            String oldNameRedirect = getRedirectFor(oldName);

            if (!isValidRevision(oldNameRedirect)) {
              return null;
            }
            RevObject old = resolve(oldNameRedirect, walk);
            if (old == null) {
              return null;
            }
            /*
             * Retain oldRevision with the old name (non-redirected-path) since it is used in
             * determining the Revision path (start index of the path from the name).
             * For example: For a master -> main redirect,
             * original path: /master/index.c is updated to /main/index.c
             * To parse the ref/path to build Revision object we look at the original path.
             */
            oldRevision = Revision.peel(oldName, old, walk);
            oldRevisionRedirected = Revision.peel(oldNameRedirect, old, walk);
            part = part.substring(dots + 2);
            b = new StringBuilder();
          } else if (firstParent > 0) {
            if (firstParent != part.length() - 2) {
              return null;
            }
            b.append(part, 0, part.length() - 2);
            String name = b.toString();
            if (!isValidRevision(name)) {
              return null;
            }

            String nameRedirected = getRedirectFor(name);
            RevObject obj = resolve(nameRedirected, walk);
            if (obj == null) {
              return null;
            }
            while (obj instanceof RevTag) {
              obj = ((RevTag) obj).getObject();
              walk.parseHeaders(obj);
            }
            if (!(obj instanceof RevCommit)) {
              return null; // Not a commit, ^! is invalid.
            }
            RevCommit c = (RevCommit) obj;
            if (c.getParentCount() > 0) {
              oldRevisionRedirected = Revision.peeled(nameRedirected + "^", c.getParent(0));
            } else {
              oldRevisionRedirected = Revision.NULL;
            }
            Result result =
                new Result(
                    Revision.peeled(nameRedirected, c),
                    oldRevisionRedirected,
                    path.substring(name.length() + 2));
            return isVisible(walk, result) ? result : null;
          }
        }
        b.append(part);

        String name = b.toString();
        if (!isValidRevision(name)) {
          return null;
        }
        String nameRedirected = getRedirectFor(name);

        RevObject obj = resolve(nameRedirected, walk);
        if (obj != null) {
          int pathStart;
          if (oldRevision == null) {
            pathStart = name.length(); // foo
          } else {
            // foo..bar (foo may be empty)
            pathStart = oldRevision.getName().length() + 2 + name.length();
          }
          Result result =
              new Result(
                  Revision.peel(nameRedirected, obj, walk),
                  oldRevisionRedirected,
                  path.substring(pathStart));
          return isVisible(walk, result) ? result : null;
        }
        first = false;
      }
      return null;
    }
  }

  private @Nullable RevObject resolve(String name, RevWalk walk) throws IOException {
    try {
      ObjectId id = repo.resolve(name);
      return id != null ? walk.parseAny(id) : null;
    } catch (AmbiguousObjectException e) {
      // TODO(dborowitz): Render a helpful disambiguation page.
      return null;
    } catch (RevisionSyntaxException | MissingObjectException e) {
      return null;
    }
  }

  private static boolean isValidRevision(String revision) {
    // Disallow some uncommon but valid revision expressions that either we
    // don't support or we represent differently in our URLs.
    return !revision.contains(":")
        && !revision.contains("^{")
        && !revision.contains("@{")
        && !revision.equals("@");
  }

  private boolean isVisible(RevWalk walk, Result result) throws IOException {
    String maybeRef = OPERATOR_SPLITTER.split(result.getRevision().getName()).iterator().next();
    if (repo.findRef(maybeRef) != null) {
      // Name contains a visible ref; skip expensive reachability check.
      return true;
    }
    ObjectId id = result.getRevision().getId();
    if (!cache.isVisible(repo, walk, access, id)) {
      return false;
    }
    if (result.getOldRevision() != null && !Revision.isNull(result.getOldRevision())) {
      return cache.isVisible(repo, walk, access, result.getOldRevision().getId(), id);
    }
    return true;
  }

  /**
   * It replaces the ref in the revision expression to the redirected refName, without changing the
   * behavior of the expression.
   *
   * <p>For eg: branch redirect {master -> main} would yield {master -> main}, {refs/heads/master^
   * -> refs/heads/main^}, {refs/heads/master^ -> refs/heads/main^}. It does expand to a full
   * refName even for shorter refNames.
   */
  private String getRedirectFor(String revisionExpression) {
    String refName = refPart(revisionExpression);
    Optional<String> redirect = branchRedirect.getRedirectBranch(repo, refName);
    if (redirect.isPresent()) {
      return redirect.get() + revisionExpression.substring(refName.length());
    }
    return revisionExpression;
  }

  private static String refPart(String revisionExpression) {
    Matcher m = END_OF_REF.matcher(revisionExpression);
    if (!m.find()) { // no terminator -> the whole string is a ref name.
      return revisionExpression;
    }
    return revisionExpression.substring(0, m.start());
  }
}
