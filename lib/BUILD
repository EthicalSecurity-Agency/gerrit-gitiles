load("@rules_java//java:defs.bzl", "java_library")

package(
    default_visibility = ["//visibility:public"],
)

[java_library(
    name = n,
    exports = ["@%s//jar" % n],
) for n in [
    "autolink",
    "commonmark",
    "commons-codec",
    "commons-lang3",
    "commons-text",
    "cm-autolink",
    "gfm-strikethrough",
    "gfm-tables",
    "html-types",
    "jsr305",
    "servlet-api",
    "gson",
    "guava",
    "guava-failureaccess",
    "ow2-asm",
    "ow2-asm-analysis",
    "ow2-asm-commons",
    "ow2-asm-tree",
    "ow2-asm-util",
]]

java_library(
    name = "prettify",
    visibility = ["//visibility:public"],
    exports = ["@java-prettify"],
)

java_library(
    name = "slf4j-api",
    exports = ["@log-api//jar"],
)

java_library(
    name = "slf4j-simple",
    runtime_deps = [
        ":slf4j-api",
        "@slf4j-simple//jar",
    ],
)

java_library(
    name = "jgit",
    visibility = ["//visibility:public"],
    exports = ["@jgit//org.eclipse.jgit:jgit"],
    runtime_deps = [
        ":slf4j-api",
        "@javaewah//jar",
    ],
)

java_library(
    name = "jgit-archive",
    visibility = ["//visibility:public"],
    exports = ["@jgit//org.eclipse.jgit.archive:jgit-archive"],
    runtime_deps = [":jgit"],
)

java_library(
    name = "jgit-junit",
    testonly = True,
    visibility = ["//visibility:public"],
    exports = ["@jgit//org.eclipse.jgit.junit:junit"],
    runtime_deps = [":jgit"],
)

java_library(
    name = "jgit-servlet",
    visibility = ["//visibility:public"],
    exports = ["@jgit//org.eclipse.jgit.http.server:jgit-servlet"],
    runtime_deps = [":jgit"],
)

java_library(
    name = "tukaani-xz",
    exports = ["@tukaani-xz//jar"],
)

java_library(
    name = "commons-compress",
    exports = ["@commons-compress//jar"],
)

java_library(
    name = "errorprone-annotations",
    visibility = ["//visibility:public"],
    exports = ["@error-prone-annotations//jar"],
)