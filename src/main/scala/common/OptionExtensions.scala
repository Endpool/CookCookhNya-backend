package common

object OptionExtensions:
  extension[A] (optA: Option[A])
    def <|>[B] (optB: Option[B]): Option[A | B] =
      optA orElse optB

