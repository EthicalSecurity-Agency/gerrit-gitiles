load("@com_googlesource_gerrit_bazlets//tools:javadoc.bzl", "java_doc")
load("@rules_java//java:defs.bzl", "java_library")
load("//tools:stamper.bzl", "stamp")

DEPS = [
    "//lib:autolink",
    "//lib:commons-lang3",
    "//lib:commons-text",
    "//lib:errorprone-annotations",
    "//lib:gson",
    "//lib:guava",
    "//lib:html-types",
    "//lib:jsr305",
    "//lib:commonmark",
    "//lib:cm-autolink",
    "//lib:gfm-tables",
    "//lib:gfm-strikethrough",
    "//lib:prettify",
    "//lib:jgit",
    "//lib:jgit-servlet",
    "//lib:slf4j-api",
    "//lib/flogger",
    "//lib/soy",
    "//java/com/google/gitiles/blame/cache",
]

DEPS_ALL = DEPS + [
    "//lib:jgit-archive",
]

java_library(
    name = "servlet-api",
    neverlink = 1,
    exports = ["//lib:servlet-api"],
)

java_library(
    name = "servlet",
    srcs = glob(["**/*.java"]),
    resource_strip_prefix = "resources",
    resources = ["//resources/com/google/gitiles"],
    visibility = ["//visibility:public"],
    deps = DEPS_ALL + [
        ":servlet-api",
    ],
)

java_doc(
    name = "servlet-javadoc",
    libs = DEPS + [
        ":servlet",
    ],
    pkgs = ["com.google.gitiles"],
    title = "Gitiles API Documentation",
    visibility = ["//visibility:public"],
)

stamp(
    name = "servlet",
    workspace = "gitiles",
)
