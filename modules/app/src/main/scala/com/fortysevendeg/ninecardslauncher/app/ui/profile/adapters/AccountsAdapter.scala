package com.fortysevendeg.ninecardslauncher.app.ui.profile.adapters

import android.support.v7.widget.RecyclerView
import android.view.{LayoutInflater, MenuItem, View, ViewGroup}
import com.fortysevendeg.macroid.extras.ImageViewTweaks._
import com.fortysevendeg.macroid.extras.ResourcesExtras._
import com.fortysevendeg.macroid.extras.TextTweaks._
import com.fortysevendeg.macroid.extras.ViewTweaks._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.UiContext
import com.fortysevendeg.ninecardslauncher.app.ui.preferences.commons.{NineCardsPreferencesValue, ShowPrintInfoOptionInAccounts}
import com.fortysevendeg.ninecardslauncher.app.ui.profile.adapters.AccountOptions._
import com.fortysevendeg.ninecardslauncher.app.ui.profile.models.{AccountSync, Device, Header}
import com.fortysevendeg.ninecardslauncher.process.theme.models.NineCardsTheme
import com.fortysevendeg.ninecardslauncher2.{R, TR, TypedFindView}
import macroid._
import macroid.FullDsl._

case class AccountsAdapter(
  items: Seq[AccountSync],
  clickListener: (Int, AccountOption, AccountSync) => Unit)(implicit activityContext: ActivityContextWrapper, uiContext: UiContext[_], theme: NineCardsTheme)
  extends RecyclerView.Adapter[ViewHolderAccountsAdapter] {

  private[this] val headerType = 0

  private[this] val itemType = 1

  override def getItemCount: Int = items.size

  override def onBindViewHolder(viewHolder: ViewHolderAccountsAdapter, position: Int): Unit =
    viewHolder.bind(items(position), position).run

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderAccountsAdapter =
    viewType match {
      case `headerType` =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.profile_account_item_header, parent, false)
        ViewHolderAccountsHeaderAdapter(view)
      case _ =>
        val view = LayoutInflater.from(parent.getContext).inflate(R.layout.profile_account_item, parent, false)
        ViewHolderAccountItemAdapter(view, clickListener)
    }

  override def getItemViewType(position: Int): Int =
    if (items(position).accountSyncType == Header) headerType else itemType

}

abstract class ViewHolderAccountsAdapter(content: View)(implicit context: ActivityContextWrapper, theme: NineCardsTheme)
  extends RecyclerView.ViewHolder(content)
  with TypedFindView {

  def bind(accountSync: AccountSync, position: Int)(implicit uiContext: UiContext[_]): Ui[_]

  override def findViewById(id: Int): View = content.findViewById(id)

}

case class ViewHolderAccountsHeaderAdapter(content: View)(implicit context: ActivityContextWrapper, theme: NineCardsTheme)
  extends ViewHolderAccountsAdapter(content) {

  lazy val title = Option(findView(TR.title))

  def bind(accountSync: AccountSync, position: Int)(implicit uiContext: UiContext[_]): Ui[_] =
    title <~ tvText(accountSync.title)

}

case class ViewHolderAccountItemAdapter(
  content: View,
  onClick: (Int, AccountOption, AccountSync) => Unit)(implicit context: ActivityContextWrapper, theme: NineCardsTheme)
  extends ViewHolderAccountsAdapter(content) {

  lazy val currentAccountOptions = Seq(
    (CopyOption, resGetString(R.string.menuAccountCopy)),
    (SyncOption, resGetString(R.string.menuAccountSync)),
    (ChangeNameOption, resGetString(R.string.menuAccountChangeName)))

  lazy val otherAccountOptions = Seq(
    (CopyOption, resGetString(R.string.menuAccountCopy)),
    (DeleteOption, resGetString(R.string.menuAccountDelete)),
    (ChangeNameOption, resGetString(R.string.menuAccountChangeName)))

  def menuOptions(isCurrent: Boolean): Seq[(AccountOption, String)] =
    if (isCurrent) currentAccountOptions else otherAccountOptions

  lazy val printDriveInfo = (PrintInfoOption, resGetString(R.string.menuAccountPrintInfo))

  lazy val showPrintDriveInfo = ShowPrintInfoOptionInAccounts.readValue(new NineCardsPreferencesValue)

  lazy val title = Option(findView(TR.profile_account_title))

  lazy val subtitle = Option(findView(TR.profile_account_subtitle))

  lazy val icon = Option(findView(TR.profile_account_action))

  def bind(accountSync: AccountSync, position: Int)(implicit uiContext: UiContext[_]): Ui[_] = {

    val isCurrent = accountSync.accountSyncType match {
      case d: Device => d.current
      case _ => false
    }

    val menuSeq = if (showPrintDriveInfo) menuOptions(isCurrent) :+ printDriveInfo else menuOptions(isCurrent)

    (title <~ tvText(accountSync.title)) ~
      (subtitle <~ tvText(accountSync.subtitle getOrElse "")) ~
      (icon <~ ivSrc(R.drawable.icon_action_bar_options_dark) <~ On.click {
        icon <~ vPopupMenuShow(
          menuSeq map {
            case (_, name) => name
          },
          onMenuItemClickListener = (item: MenuItem) => {
            menuSeq lift item.getOrder foreach {
              case (option, _) => onClick(getAdapterPosition, option, accountSync)
            }
            true
          }
        )
      })
  }

}

object AccountOptions {

  sealed trait AccountOption

  case object CopyOption extends AccountOption

  case object SyncOption extends AccountOption

  case object DeleteOption extends AccountOption

  case object ChangeNameOption extends AccountOption

  case object PrintInfoOption extends AccountOption

}