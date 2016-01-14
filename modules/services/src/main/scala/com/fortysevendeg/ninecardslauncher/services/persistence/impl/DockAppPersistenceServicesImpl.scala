package com.fortysevendeg.ninecardslauncher.services.persistence.impl

import com.fortysevendeg.ninecardslauncher.commons.NineCardExtensions._
import com.fortysevendeg.ninecardslauncher.repository.provider.DockAppEntity
import com.fortysevendeg.ninecardslauncher.services.persistence._
import com.fortysevendeg.ninecardslauncher.services.persistence.conversions.Conversions
import com.fortysevendeg.ninecardslauncher.services.persistence.models.IterableDockApps

trait DockAppPersistenceServicesImpl {

  self: Conversions with PersistenceDependencies with ImplicitsPersistenceServiceExceptions =>

  def createOrUpdateDockApp(request: CreateOrUpdateDockAppRequest) =
    (for {
      dockApps <- dockAppRepository.fetchDockApps(where = s"${DockAppEntity.position} = ?", whereParams = Seq(request.position.toString))
      app = dockApps.headOption
      id <- app map { a =>
        updateDockApp(a.id, request)
      } getOrElse addDockApp(request)
    } yield id).resolve[PersistenceServiceException]

  def deleteAllDockApps() =
    (for {
      deleted <- dockAppRepository.deleteDockApps()
    } yield deleted).resolve[PersistenceServiceException]

  def deleteDockApp(request: DeleteDockAppRequest) =
    (for {
      deleted <- dockAppRepository.deleteDockApp(toRepositoryDockApp(request.dockApp))
    } yield deleted).resolve[PersistenceServiceException]

  def fetchDockApps =
    (for {
      dockAppItems <- dockAppRepository.fetchDockApps()
    } yield dockAppItems map toDockApp).resolve[PersistenceServiceException]

  def fetchIterableDockApps =
    (for {
      iter <- dockAppRepository.fetchIterableDockApps()
    } yield new IterableDockApps(iter)).resolve[PersistenceServiceException]

  def findDockAppById(request: FindDockAppByIdRequest) =
    (for {
      maybeDockApp <- dockAppRepository.findDockAppById(request.id)
    } yield maybeDockApp map toDockApp).resolve[PersistenceServiceException]

  private[this] def addDockApp(request: CreateOrUpdateDockAppRequest) =
    (for {
      dockApp <- dockAppRepository.addDockApp(toRepositoryDockAppData(request))
    } yield dockApp.id).resolve[PersistenceServiceException]

  private[this] def updateDockApp(id: Int, request: CreateOrUpdateDockAppRequest) =
    (for {
      updated <- dockAppRepository.updateDockApp(toRepositoryDockApp(id, request))
    } yield updated).resolve[PersistenceServiceException]
  
}
