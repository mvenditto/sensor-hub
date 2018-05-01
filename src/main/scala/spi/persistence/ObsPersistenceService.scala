package spi.persistence

import spi.service.Service

trait ObsPersistenceService extends Service {

  def getObsStorage: ObsStorage

  // TODO persistence API

}
