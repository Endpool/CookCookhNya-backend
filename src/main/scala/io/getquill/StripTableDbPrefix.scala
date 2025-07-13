package io.getquill

object StripTableDbPrefix extends NamingStrategy:
  override inline def default(s: String) = s
  override inline def table(s: String) = s.stripPrefix("Db")

