package es.c0n1j.blocfone.data

internal fun escapeLikeLiteral(value: String): String = value
    .replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")
