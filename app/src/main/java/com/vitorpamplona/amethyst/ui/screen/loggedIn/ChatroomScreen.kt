package com.vitorpamplona.amethyst.ui.screen.loggedIn

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.ChatroomKey
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.ServersAvailable
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.NostrChatroomDataSource
import com.vitorpamplona.amethyst.service.model.ChatMessageEvent
import com.vitorpamplona.amethyst.ui.actions.NewPostViewModel
import com.vitorpamplona.amethyst.ui.actions.PostButton
import com.vitorpamplona.amethyst.ui.actions.UploadFromGallery
import com.vitorpamplona.amethyst.ui.components.ObserveDisplayNip05Status
import com.vitorpamplona.amethyst.ui.note.ClickableUserPicture
import com.vitorpamplona.amethyst.ui.note.DisplayRoomSubject
import com.vitorpamplona.amethyst.ui.note.DisplayUserSetAsSubject
import com.vitorpamplona.amethyst.ui.note.LoadUser
import com.vitorpamplona.amethyst.ui.note.NonClickableUserPictures
import com.vitorpamplona.amethyst.ui.note.QuickActionAlertDialog
import com.vitorpamplona.amethyst.ui.note.UserCompose
import com.vitorpamplona.amethyst.ui.note.UsernameDisplay
import com.vitorpamplona.amethyst.ui.screen.NostrChatroomFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.RefreshingChatroomFeedView
import com.vitorpamplona.amethyst.ui.theme.EditFieldBorder
import com.vitorpamplona.amethyst.ui.theme.EditFieldModifier
import com.vitorpamplona.amethyst.ui.theme.EditFieldTrailingIconModifier
import com.vitorpamplona.amethyst.ui.theme.Size30Modifier
import com.vitorpamplona.amethyst.ui.theme.Size34dp
import com.vitorpamplona.amethyst.ui.theme.StdPadding
import com.vitorpamplona.amethyst.ui.theme.placeholderText
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatroomScreen(
    roomId: String?,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    if (roomId == null) return

    LoadRoom(roomId, accountViewModel) {
        it?.let {
            PrepareChatroomViewModels(
                room = it,
                accountViewModel = accountViewModel,
                nav = nav
            )
        }
    }
}

@Composable
fun ChatroomScreenByAuthor(
    authorPubKeyHex: String?,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    if (authorPubKeyHex == null) return

    LoadRoomByAuthor(authorPubKeyHex, accountViewModel) {
        it?.let {
            PrepareChatroomViewModels(
                room = it,
                accountViewModel = accountViewModel,
                nav = nav
            )
        }
    }
}

@Composable
fun LoadRoom(roomId: String, accountViewModel: AccountViewModel, content: @Composable (ChatroomKey?) -> Unit) {
    var room by remember(roomId) {
        mutableStateOf<ChatroomKey?>(null)
    }

    if (room == null) {
        LaunchedEffect(key1 = roomId) {
            launch(Dispatchers.IO) {
                val newRoom = accountViewModel.userProfile().privateChatrooms.keys.firstOrNull { it.hashCode().toString() == roomId }
                if (room != newRoom) {
                    room = newRoom
                }
            }
        }
    }

    content(room)
}

@Composable
fun LoadRoomByAuthor(authorPubKeyHex: String, accountViewModel: AccountViewModel, content: @Composable (ChatroomKey?) -> Unit) {
    val room by remember(authorPubKeyHex) {
        mutableStateOf<ChatroomKey?>(ChatroomKey(persistentSetOf(authorPubKeyHex)))
    }

    content(room)
}

@Composable
fun PrepareChatroomViewModels(room: ChatroomKey, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val feedViewModel: NostrChatroomFeedViewModel = viewModel(
        key = room.hashCode().toString() + "ChatroomViewModels",
        factory = NostrChatroomFeedViewModel.Factory(
            room,
            accountViewModel.account
        )
    )

    val newPostModel: NewPostViewModel = viewModel()
    newPostModel.account = accountViewModel.account
    newPostModel.requiresNIP24 = room.users.size > 1
    if (newPostModel.requiresNIP24) {
        newPostModel.nip24 = true
    }

    ChatroomScreen(
        room = room,
        feedViewModel = feedViewModel,
        newPostModel = newPostModel,
        accountViewModel = accountViewModel,
        nav = nav
    )
}

@Composable
fun ChatroomScreen(
    room: ChatroomKey,
    feedViewModel: NostrChatroomFeedViewModel,
    newPostModel: NewPostViewModel,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val context = LocalContext.current

    NostrChatroomDataSource.loadMessagesBetween(accountViewModel.account, room)

    val lifeCycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(room, accountViewModel) {
        launch(Dispatchers.IO) {
            NostrChatroomDataSource.start()
            feedViewModel.invalidateData()

            newPostModel.imageUploadingError.collect { error ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DisposableEffect(room, accountViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                println("Private Message Start")
                NostrChatroomDataSource.start()
                feedViewModel.invalidateData()
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                println("Private Message Stop")
                NostrChatroomDataSource.stop()
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(Modifier.fillMaxHeight()) {
        val replyTo = remember { mutableStateOf<Note?>(null) }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 0.dp)
                .weight(1f, true)
        ) {
            RefreshingChatroomFeedView(
                viewModel = feedViewModel,
                accountViewModel = accountViewModel,
                nav = nav,
                routeForLastRead = "Room/${room.hashCode()}",
                onWantsToReply = {
                    replyTo.value = it
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        replyTo.value?.let {
            DisplayReplyingToNote(it, accountViewModel, nav) {
                replyTo.value = null
            }
        }

        val scope = rememberCoroutineScope()

        // LAST ROW
        PrivateMessageEditFieldRow(newPostModel, isPrivate = true, accountViewModel) {
            scope.launch(Dispatchers.IO) {
                if (newPostModel.nip24 || room.users.size > 1 || replyTo.value?.event is ChatMessageEvent) {
                    accountViewModel.account.sendNIP24PrivateMessage(
                        message = newPostModel.message.text,
                        toUsers = room.users.toList(),
                        replyingTo = replyTo.value,
                        mentions = null,
                        wantsToMarkAsSensitive = false
                    )
                } else {
                    accountViewModel.account.sendPrivateMessage(
                        message = newPostModel.message.text,
                        toUser = room.users.first(),
                        replyingTo = replyTo.value,
                        mentions = null,
                        wantsToMarkAsSensitive = false
                    )
                }

                newPostModel.message = TextFieldValue("")
                replyTo.value = null
                feedViewModel.sendToTop()
            }
        }
    }
}

@Composable
fun PrivateMessageEditFieldRow(
    channelScreenModel: NewPostViewModel,
    isPrivate: Boolean,
    accountViewModel: AccountViewModel,
    onSendNewMessage: () -> Unit
) {
    Row(
        modifier = EditFieldModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current

        MyTextField(
            value = channelScreenModel.message,
            onValueChange = {
                channelScreenModel.updateMessage(it)
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences
            ),
            shape = EditFieldBorder,
            modifier = Modifier.weight(1f, true),
            placeholder = {
                Text(
                    text = stringResource(R.string.reply_here),
                    color = MaterialTheme.colors.placeholderText
                )
            },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
            trailingIcon = {
                PostButton(
                    onPost = {
                        onSendNewMessage()
                    },
                    isActive = channelScreenModel.message.text.isNotBlank() && !channelScreenModel.isUploadingImage,
                    modifier = EditFieldTrailingIconModifier
                )
            },
            leadingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                    UploadFromGallery(
                        isUploading = channelScreenModel.isUploadingImage,
                        tint = MaterialTheme.colors.placeholderText,
                        modifier = Modifier
                            .size(30.dp)
                            .padding(start = 2.dp)
                    ) {
                        val fileServer = if (isPrivate) {
                            // TODO: Make private servers
                            when (accountViewModel.account.defaultFileServer) {
                                ServersAvailable.NOSTR_BUILD -> ServersAvailable.NOSTR_BUILD
                                ServersAvailable.NOSTRIMG -> ServersAvailable.NOSTRIMG
                                ServersAvailable.NOSTRFILES_DEV -> ServersAvailable.NOSTRFILES_DEV
                                ServersAvailable.NOSTRCHECK_ME -> ServersAvailable.NOSTRCHECK_ME

                                ServersAvailable.NOSTR_BUILD_NIP_94 -> ServersAvailable.NOSTR_BUILD
                                ServersAvailable.NOSTRIMG_NIP_94 -> ServersAvailable.NOSTRIMG
                                ServersAvailable.NOSTRFILES_DEV_NIP_94 -> ServersAvailable.NOSTRFILES_DEV
                                ServersAvailable.NOSTRCHECK_ME_NIP_94 -> ServersAvailable.NOSTRCHECK_ME

                                ServersAvailable.NIP95 -> ServersAvailable.NOSTR_BUILD
                            }
                        } else {
                            accountViewModel.account.defaultFileServer
                        }

                        channelScreenModel.upload(it, "", false, fileServer, context)
                    }

                    var wantsToActivateNIP24 by remember {
                        mutableStateOf(false)
                    }

                    if (wantsToActivateNIP24) {
                        NewFeatureNIP24AlertDialog(
                            accountViewModel = accountViewModel,
                            onConfirm = {
                                channelScreenModel.toggleNIP04And24()
                            },
                            onDismiss = {
                                wantsToActivateNIP24 = false
                            }
                        )
                    }

                    IconButton(
                        modifier = Size30Modifier,
                        onClick = {
                            if (!accountViewModel.hideNIP24WarningDialog && !channelScreenModel.nip24 && !channelScreenModel.requiresNIP24) {
                                wantsToActivateNIP24 = true
                            } else {
                                channelScreenModel.toggleNIP04And24()
                            }
                        }
                    ) {
                        if (channelScreenModel.nip24) {
                            Icon(
                                painter = painterResource(id = R.drawable.incognito),
                                null,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp),
                                tint = Color.Green
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.incognito_off),
                                null,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp),
                                tint = MaterialTheme.colors.placeholderText
                            )
                        }
                    }
                }
            },
            colors = TextFieldDefaults.textFieldColors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun NewFeatureNIP24AlertDialog(accountViewModel: AccountViewModel, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()

    QuickActionAlertDialog(
        title = stringResource(R.string.new_feature_nip24_might_not_be_available_title),
        textContent = stringResource(R.string.new_feature_nip24_might_not_be_available_description),
        buttonIconResource = R.drawable.incognito,
        buttonText = stringResource(R.string.new_feature_nip24_activate),
        onClickDoOnce = {
            scope.launch(Dispatchers.IO) {
                onConfirm()
            }
            onDismiss()
        },
        onClickDontShowAgain = {
            scope.launch(Dispatchers.IO) {
                onConfirm()
                accountViewModel.dontShowNIP24WarningDialog()
            }
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
fun ChatroomHeader(
    room: ChatroomKey,
    modifier: Modifier = StdPadding,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    if (room.users.size == 1) {
        LoadUser(baseUserHex = room.users.first()) { baseUser ->
            if (baseUser != null) {
                ChatroomHeader(baseUser = baseUser, modifier = modifier, accountViewModel = accountViewModel, nav = nav)
            }
        }
    } else {
        GroupChatroomHeader(room = room, modifier = modifier, accountViewModel = accountViewModel, nav = nav)
    }
}

@Composable
fun ChatroomHeader(
    baseUser: User,
    modifier: Modifier = StdPadding,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = { nav("User/${baseUser.pubkeyHex}") }
            )
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClickableUserPicture(
                    baseUser = baseUser,
                    accountViewModel = accountViewModel,
                    size = Size34dp
                )

                Column(modifier = Modifier.padding(start = 10.dp)) {
                    UsernameDisplay(baseUser)
                    ObserveDisplayNip05Status(baseUser)
                }
            }
        }

        Divider(
            thickness = 0.25.dp
        )
    }
}

@Composable
fun GroupChatroomHeader(
    room: ChatroomKey,
    modifier: Modifier = StdPadding,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier.clickable {
                expanded.value = !expanded.value
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NonClickableUserPictures(
                    users = room.users,
                    accountViewModel = accountViewModel,
                    size = Size34dp
                )

                Column(modifier = Modifier.padding(start = 10.dp)) {
                    RoomNameOnlyDisplay(room, Modifier, accountViewModel.userProfile())
                    DisplayUserSetAsSubject(room, FontWeight.Normal)
                }
            }

            if (expanded.value) {
                LongRoomHeader(room, accountViewModel, nav)
            }
        }

        Divider(
            thickness = 0.25.dp
        )
    }
}

@Composable
fun LongRoomHeader(room: ChatroomKey, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val list = remember(room) {
        room.users.toPersistentList()
    }

    Row(modifier = Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(id = R.string.messages_group_descriptor),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(
            bottom = 10.dp
        ),
        state = rememberLazyListState()
    ) {
        itemsIndexed(list, key = { _, item -> item }) { _, item ->
            LoadUser(baseUserHex = item) {
                if (it != null) {
                    UserCompose(baseUser = it, accountViewModel = accountViewModel, nav = nav)
                }
            }
        }
    }
}

@Composable
fun RoomNameOnlyDisplay(room: ChatroomKey, modifier: Modifier, loggedInUser: User) {
    val roomSubject by loggedInUser.live().messages.map {
        it.user.privateChatrooms[room]?.subject
    }.distinctUntilChanged().observeAsState(loggedInUser.privateChatrooms[room]?.subject)

    Crossfade(targetState = roomSubject, modifier) {
        if (it != null && it.isNotBlank()) {
            DisplayRoomSubject(it)
        }
    }
}
