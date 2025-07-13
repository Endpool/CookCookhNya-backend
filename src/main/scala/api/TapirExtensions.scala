package api

import sttp.tapir.Endpoint

object TapirExtensions:
  extension[T1, T2, T3, T4, T5](endpoint: Endpoint[T1, T2, T3, T4, T5])
    def subTag(subTag: String): Endpoint[T1, T2, T3, T4, T5] =
      val newTag = endpoint.info.tags.headOption match
        case None => subTag
        case Some(tag) => tag + ':' + subTag
      endpoint.withTag(newTag)

    def superTag(superTag: String): Endpoint[T1, T2, T3, T4, T5] =
      val newTag = endpoint.info.tags.headOption match
        case None => superTag
        case Some(tag) => superTag + ':' + tag
      endpoint.withTag(newTag)
