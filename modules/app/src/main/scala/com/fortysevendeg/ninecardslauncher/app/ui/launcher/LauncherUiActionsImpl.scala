package com.fortysevendeg.ninecardslauncher.app.ui.launcher

import android.app.Activity
import android.content.ClipData
import android.graphics.Point
import android.support.v4.app.{Fragment, FragmentManager}
import android.support.v7.app.AppCompatActivity
import android.view.DragEvent._
import android.view.View.OnDragListener
import android.view.{DragEvent, View, WindowManager}
import android.widget.ImageView
import com.fortysevendeg.macroid.extras.DeviceVersion.{KitKat, Lollipop}
import com.fortysevendeg.macroid.extras.ResourcesExtras._
import com.fortysevendeg.macroid.extras.ViewTweaks._
import com.fortysevendeg.macroid.extras.UIActionsExtras._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.Constants._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.SnailsCommons._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.UiOps._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.ViewOps._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.CommonsTweak._
import com.fortysevendeg.ninecardslauncher.app.ui.commons._
import com.fortysevendeg.ninecardslauncher.app.ui.components.layouts.tweaks.LauncherWorkSpacesTweaks._
import com.fortysevendeg.ninecardslauncher.app.ui.components.layouts.tweaks.DockAppsPanelLayoutTweaks._
import com.fortysevendeg.ninecardslauncher.app.ui.launcher.collection.CollectionsUiActions
import com.fortysevendeg.ninecardslauncher.app.ui.launcher.drag.AppDrawerIconShadowBuilder
import com.fortysevendeg.ninecardslauncher.app.ui.launcher.drawer.DrawerUiActions
import com.fortysevendeg.ninecardslauncher.app.ui.launcher.snails.LauncherSnails._
import com.fortysevendeg.ninecardslauncher.app.ui.launcher.types.AddItemToCollection
import com.fortysevendeg.ninecardslauncher.app.ui.commons.CommonsExcerpt._
import com.fortysevendeg.ninecardslauncher.app.ui.components.drawables.RippleCollectionDrawable
import com.fortysevendeg.ninecardslauncher.process.commons.models.Collection
import com.fortysevendeg.ninecardslauncher.process.device.models.{Contact, LastCallsContact, _}
import com.fortysevendeg.ninecardslauncher.process.device.{GetAppOrder, GetByName}
import com.fortysevendeg.ninecardslauncher.process.theme.models.NineCardsTheme
import com.fortysevendeg.ninecardslauncher2.{R, TR, TypedFindView}
import macroid._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LauncherUiActionsImpl
  extends LauncherUiActions
  with CollectionsUiActions
  with DrawerUiActions {

  self: TypedFindView with SystemBarsTint with Contexts[AppCompatActivity] =>

  implicit val uiContext: UiContext[Activity]

  implicit val presenter: LauncherPresenter

  implicit lazy val theme: NineCardsTheme = presenter.getTheme

  implicit val managerContext: FragmentManagerContext[Fragment, FragmentManager]

  lazy val foreground = Option(findView(TR.launcher_foreground))

  override def initialize: Ui[Any] =
    Ui(initAllSystemBarsTint) ~
      prepareBars ~
      initCollectionsUi ~
      initDrawerUi ~
      (root <~ dragListener())

  override def addCollection(collection: Collection): Ui[Any] = uiActionCollection(Add, collection)

  override def removeCollection(collection: Collection): Ui[Any] = uiActionCollection(Remove, collection)

  override def reloadDockApps(dockApp: DockApp): Ui[Any] = dockAppsPanel <~ daplReload(dockApp)

  override def showAddItemMessage(nameCollection: String): Ui[Any] = showMessage(R.string.itemAddedToCollectionSuccessful, Seq(nameCollection))

  override def showContactUsError(): Ui[Any] = showMessage(R.string.contactUsError)

  override def showMinimumOneCollectionMessage(): Ui[Any] = showMessage(R.string.minimumOneCollectionMessage)

  override def showLoading(): Ui[Any] = showCollectionsLoading

  override def goToPreviousScreen(): Ui[Any]= {
    val canMoveToPreviousScreen = (workspaces ~> lwsCanMoveToPreviousScreen()).get getOrElse false
    goToPreviousWorkspace().ifUi(canMoveToPreviousScreen)
  }

  override def goToNextScreen(): Ui[Any] = {
    val canMoveToNextScreen = (workspaces ~> lwsCanMoveToNextScreen()).get getOrElse false
    goToNextWorkspace().ifUi(canMoveToNextScreen)
  }

  override def goToPreviousScreenReordering(): Ui[Any] = {
    val canMoveToPreviousScreen = (workspaces ~> lwsCanMoveToPreviousScreen()).get getOrElse false
    (goToPreviousWorkspace() ~ (workspaces <~ lwsPrepareItemsScreenInReorder(numSpaces - 1))).ifUi(canMoveToPreviousScreen)
  }

  override def goToNextScreenReordering(): Ui[Any] = {
    val canMoveToNextScreen = (workspaces ~> lwsCanMoveToNextScreen()).get getOrElse false
    (goToNextWorkspace() ~ (workspaces <~ lwsPrepareItemsScreenInReorder(0))).ifUi(canMoveToNextScreen)
  }

  override def loadCollections(collections: Seq[Collection], apps: Seq[DockApp]): Ui[Any] =
    createCollections(collections, apps)

  def reloadCollectionsAfterReorder(from: Int, to: Int): Ui[Any] = reloadReorderedCollections(from, to)

  def reloadCollectionsFailed(): Ui[Any] = reloadCollections()

  override def showUserProfile(email: Option[String], name: Option[String], avatarUrl: Option[String], coverPhotoUrl: Option[String]): Ui[Any] =
    userProfileMenu(email, name, avatarUrl, coverPhotoUrl)

  override def reloadAppsInDrawer(
    apps: IterableApps,
    getAppOrder: GetAppOrder = GetByName,
    counters: Seq[TermCounter] = Seq.empty): Ui[Any] =
    addApps(
      apps = apps,
      clickListener = (app: App) => presenter.openApp(app),
      longClickListener = (view: View, app: App) => {
        presenter.startAddItemToCollection(app)
        (view <~ startDrag()).run
      },
      getAppOrder = getAppOrder,
      counters = counters)

  override def reloadContactsInDrawer(
    contacts: IterableContacts,
    counters: Seq[TermCounter] = Seq.empty): Ui[_] =
    addContacts(
      contacts = contacts,
      clickListener = (contact: Contact) => presenter.openContact(contact),
      longClickListener = (view: View, contact: Contact) => {
        presenter.startAddItemToCollection(contact)
        (view <~ startDrag()).run
      },
      counters = counters)

  override def reloadLastCallContactsInDrawer(contacts: Seq[LastCallsContact]): Ui[Any] =
    addLastCallContacts(contacts, (contact: LastCallsContact) => presenter.openLastCall(contact))

  override def rippleToCollection(color: Int, point: Point): Ui[Future[Any]] = {
    val y = KitKat.ifSupportedThen (point.y - getStatusBarHeight) getOrElse point.y
    val background = new RippleCollectionDrawable(point.x, y, color)
    (foreground <~
      vVisible <~
      vBackground(background)) ~
      background.start()
  }

  def resetFromCollection(): Ui[Any] = foreground <~ vBlankBackground <~ vGone

  override def back: Ui[Any] =
    if (isDrawerTabsOpened) {
      closeDrawerTabs
    } else if (isMenuVisible) {
      closeMenu()
    } else if (isDrawerVisible) {
      revealOutDrawer
    } else if (isActionShowed) {
      unrevealActionFragment
    } else if (isCollectionMenuVisible) {
      closeCollectionMenu()
    } else {
      Ui.nop
    }

  override def resetAction: Ui[Any] = turnOffFragmentContent

  override def destroyAction: Ui[Any] = Ui(removeActionFragment)

  override def logout: Ui[Any] = cleanWorkspaces() ~ Ui(presenter.goToWizard())

  override def closeTabs: Ui[Any] = closeDrawerTabs

  override def startReorder: Ui[Any] =
    (dockAppsPanel <~ fadeOut()) ~
      (searchPanel <~ fadeOut()) ~
      (collectionActionsPanel <~ fadeIn())

  override def endReorder: Ui[Any] =
    (dockAppsPanel <~ fadeIn()) ~
      (searchPanel <~ fadeIn()) ~
      (collectionActionsPanel <~~ fadeOut())

  override def startAddItem: Ui[Any] =
    revealOutDrawer ~
    (searchPanel <~ fadeOut()) ~
      (collectionActionsPanel <~ fadeIn())

  override def endAddItem: Ui[Any] =
    (searchPanel <~ fadeIn()) ~
      (collectionActionsPanel <~~ fadeOut())

  private[this] def fadeIn() = vVisible + vAlpha(0) ++ applyAnimation(alpha = Some(1))

  private[this] def fadeOut() = applyAnimation(alpha = Some(0)) + vInvisible

  override def isTabsOpened: Boolean = isDrawerTabsOpened

  override def canRemoveCollections: Boolean = getCountCollections > 1

  override def getCollection(position: Int): Option[Collection] = getCollections.lift(position)

  override def isEmptyCollectionsInWorkspace: Boolean = isEmptyCollections

  def turnOffFragmentContent: Ui[_] =
    fragmentContent <~ vClickable(false)

  def reloadPager(currentPage: Int) = Transformer {
    case imageView: ImageView if imageView.isPosition(currentPage) =>
      imageView <~ vActivated(true) <~~ pagerAppear
    case imageView: ImageView =>
      imageView <~ vActivated(false)
  }

  private[this] def prepareBars =
    KitKat.ifSupportedThen {
      val activity = activityContextWrapper.getOriginal
      val paddingDefault = resGetDimensionPixelSize(R.dimen.padding_default)
      val sbHeight = getStatusBarHeight
      val nbHeight = getNavigationBarHeight
      val elevation = resGetDimensionPixelSize(R.dimen.elevation_fab_button)
      Ui(activity.getWindow.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)) ~
        (content <~ vPadding(0, sbHeight, 0, nbHeight)) ~
        (menuCollectionRoot <~ vPadding(0, sbHeight, 0, nbHeight)) ~
        (drawerContent <~ vPadding(0, sbHeight, 0, nbHeight)) ~
        (actionFragmentContent <~
          vPadding(paddingDefault, paddingDefault + sbHeight, paddingDefault, paddingDefault + nbHeight)) ~
        (drawerLayout <~ vBackground(R.drawable.background_workspace)) ~
        (Lollipop.ifSupportedThen {
          actionFragmentContent <~ vElevation(elevation)
        } getOrElse Ui.nop)
    } getOrElse Ui.nop

  private[this] def dragListener(): Tweak[View] = Tweak[View] { view =>
    view.setOnDragListener(new OnDragListener {
      val dragAreaKey = "drag-area"
      override def onDrag(v: View, event: DragEvent): Boolean = {
        val dragArea = v.getField[DragArea](dragAreaKey) getOrElse NoDragArea
        (event.getLocalState, event.getAction, (searchPanel ~> height).get, (dockAppsPanel ~> height).get) match {
          case (DragObject(_, AddItemToCollection), ACTION_DRAG_ENDED, _ , _) =>
            (v <~ vAddField(dragAreaKey, NoDragArea)).run
            presenter.endAddItem()
          case (DragObject(_, AddItemToCollection), _, Some(topBar), Some(bottomBar)) =>
            val height = KitKat.ifSupportedThen (view.getHeight - getStatusBarHeight) getOrElse view.getHeight
            val top = KitKat.ifSupportedThen (topBar + getStatusBarHeight) getOrElse topBar
            // Project location to views
            val x = event.getX
            val y = event.getY
            val currentDragArea = if (y < top) ActionsDragArea else if (y > height - bottomBar) DockAppsDragArea else WorkspacesDragArea

            val (action, area) = if (dragArea != currentDragArea) {
              (v <~ vAddField(dragAreaKey, currentDragArea)).run
              (ACTION_DRAG_EXITED, dragArea)
            } else {
              (event.getAction, currentDragArea)
            }

            area match {
              case WorkspacesDragArea =>
                // Project to workspace
                (workspaces <~ lwsDragDispatcher(action, x, y - top)).run
              case DockAppsDragArea =>
                // Project to dock apps
                (dockAppsPanel <~ daplDragDispatcher(action, x, y - (height - bottomBar))).run
              case _ =>
            }
          case _ =>
        }
        true
      }
    })
  }

  private[this] def startDrag(): Tweak[View] = Tweak[View] { view =>
    val dragData = ClipData.newPlainText("", "")
    val shadow = new AppDrawerIconShadowBuilder(view)
    view.startDrag(dragData, shadow, DragObject(shadow, AddItemToCollection), 0)
  }

}

sealed trait DragArea

case object ActionsDragArea extends DragArea

case object WorkspacesDragArea extends DragArea

case object DockAppsDragArea extends DragArea

case object NoDragArea extends DragArea