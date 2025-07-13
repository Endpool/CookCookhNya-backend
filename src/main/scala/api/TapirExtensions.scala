package api

import sttp.tapir.Endpoint

object TapirExtensions:
  extension[SEC_IN, IN, ERR_OUT, OUT, R](endpoint: Endpoint[SEC_IN, IN, ERR_OUT, OUT, R])
    def subTag(subTag: String): Endpoint[SEC_IN, IN, ERR_OUT, OUT, R] =
      val newTag = endpoint.info.tags.headOption match
        case None => subTag
        case Some(tag) => tag + ':' + subTag
      endpoint.withTag(newTag)

    def superTag(superTag: String): Endpoint[SEC_IN, IN, ERR_OUT, OUT, R] =
      val newTag = endpoint.info.tags.headOption match
        case None => superTag
        case Some(tag) => superTag + ':' + tag
      endpoint.withTag(newTag)
