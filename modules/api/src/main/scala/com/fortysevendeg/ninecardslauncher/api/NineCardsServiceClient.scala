package com.fortysevendeg.ninecardslauncher.api

import com.fortysevendeg.ninecardslauncher.api.services.{UserConfigServiceClient, SharedCollectionsServiceClient}
import com.fortysevendeg.rest.client.ServiceClient

trait NineCardsServiceClient
  extends ServiceClient
  with UserConfigServiceClient
  with SharedCollectionsServiceClient