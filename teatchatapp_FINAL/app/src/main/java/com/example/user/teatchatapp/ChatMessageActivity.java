package com.example.user.teatchatapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
//import android.widget.Toolbar;
import android.support.v7.widget.Toolbar;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bhargavms.dotloader.DotLoader;
import com.example.user.teatchatapp.Adapter.ChatMessageAdapter;
import com.example.user.teatchatapp.Common.Common;
import com.example.user.teatchatapp.Holder.QBChatDialogHolder;
import com.example.user.teatchatapp.Holder.QBChatMessagesHolder;
import com.example.user.teatchatapp.Holder.QBUnreadMessageHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBChatDialogParticipantListener;
import com.quickblox.chat.listeners.QBChatDialogTypingListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.model.QBPresence;
import com.quickblox.chat.request.QBDialogRequestBuilder;
import com.quickblox.chat.request.QBMessageGetBuilder;
import com.quickblox.chat.request.QBMessageUpdateBuilder;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestUpdateBuilder;
import com.squareup.picasso.Picasso;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

public class ChatMessageActivity extends AppCompatActivity implements QBChatDialogMessageListener{

    QBChatDialog qbChatDialog;
    ListView lstChatMessages;
    ImageButton submitButton;
    EditText edtContent;

    ChatMessageAdapter adapter;

    //Part 9 - Update Online User
    ImageView img_online_count;
    TextView txt_online_count;

    //Variables for Edit/Delete message
    int contextMenuIndexClicked = -1;
    boolean isEditMode = false;
    QBChatMessage edtMessage;

    //Update dialog
    Toolbar toolbar;

    //Part 10 Set Avatar for Dialog
    ImageView dialog_avatar;

    //Part 12 Typing
    DotLoader dotloader;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if(qbChatDialog.getType() == QBDialogType.GROUP || qbChatDialog.getType() == QBDialogType.PUBLIC_GROUP)
            getMenuInflater().inflate(R.menu.chat_message_group_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.chat_group_edit_name:
                editNameGroup();
                break;
            case R.id.chat_group_add_user:
                addUser();
                break;
            case R.id.chat_group_remove_user:
                removeUser();
                break;
        }
        return true;
    }

    private void removeUser() {
        Intent intent = new Intent(this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_REMOVE_MODE);
        startActivity(intent);
    }

    //get all user not join Chat Group and show as list
    private void addUser() {
        Intent intent = new Intent(this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_ADD_MODE);
        startActivity(intent);
    }

    //create alert dialog where user input new group name and update group after user press OK
    private void editNameGroup() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_group_layout,null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(view);
        final EditText newName = (EditText)view.findViewById(R.id.edt_new_group_name);

        //Set Dialog message 彈出確認提示視窗(Cancel , Ok)
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        qbChatDialog.setName(newName.getText().toString()); //set new name for dialog

                        QBDialogRequestBuilder requestBuilder = new QBDialogRequestBuilder();
                        QBRestChatService.updateGroupChatDialog(qbChatDialog,requestBuilder)
                                .performAsync(new QBEntityCallback<QBChatDialog>() {
                                    @Override
                                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                        Toast.makeText(ChatMessageActivity.this, "Group name edited",Toast.LENGTH_SHORT).show();
                                        toolbar.setTitle(qbChatDialog.getName());
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        Toast.makeText(getBaseContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        //Create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        //Get index context menu click (set index of context menu clicked to global variable)
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        contextMenuIndexClicked = info.position;

        switch (item.getItemId()){
            case R.id.chat_message_update_message:
                updateMessage();
                break;
            case R.id.chat_message_delete_message:
                deleteMessage();
                break;
        }
        return true;
    }

    private void deleteMessage() {

        //ProgressBar 影片part7 19:20
        final ProgressBar progressBar_cycle;
        progressBar_cycle = findViewById(R.id.chat_message_progressBar_cyclic);
        progressBar_cycle.setVisibility(View.VISIBLE);

        edtMessage = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId())
                .get(contextMenuIndexClicked);

        QBRestChatService.deleteMessage(edtMessage.getId(),false).performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                retrieveMessage();
                //mDialog.dismiss();
                progressBar_cycle.setVisibility(View.GONE);
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    private void updateMessage() {
        //Set message for edit text (user index from context menu, we will get chat message from cache)
        edtMessage = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId())
                .get(contextMenuIndexClicked);
        edtContent.setText(edtMessage.getBody());
        isEditMode = true; // Set Edit mode to true
    } // 編輯修改 message

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.char_message_context_menu,menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message);

        final ProgressBar progressBar_cycle;
        progressBar_cycle = findViewById(R.id.chat_message_progressBar_cyclic);
        progressBar_cycle.setVisibility(View.GONE);//一開始隱藏

        initViews();

        initChatDialogs();

        //Load all message of this dialog
        retrieveMessage();

        //設定訊息發送按鈕
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //訊息欄有內容才能送出 (空的則不能發送訊息)
                if (!edtContent.getText().toString().isEmpty()) {

                    //btnSend have 2 mode :  new mode & edit mode
                    if (!isEditMode) {
                        QBChatMessage chatMessage = new QBChatMessage();
                        chatMessage.setBody(edtContent.getText().toString());
                        chatMessage.setSenderId(QBChatService.getInstance().getUser().getId());
                        chatMessage.setSaveToHistory(true);

                        try {
                            qbChatDialog.sendMessage(chatMessage);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }

                        //Fix Private chat don't show message
                        if (qbChatDialog.getType() == QBDialogType.PRIVATE) {
                            //Cache Message (Save message to cache and  refresh list view)
                            QBChatMessagesHolder.getInstance().putMessage(qbChatDialog.getDialogId(), chatMessage);
                            ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(chatMessage.getDialogId());

                            adapter = new ChatMessageAdapter(getBaseContext(), messages);
                            lstChatMessages.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        }

                        //Remove text from edit text
                        edtContent.setText("");
                        edtContent.setFocusable(true);

                    } else {
                        //final ProgressBar 影片part7 10:10
                        final ProgressBar progressBar_cycle;
                        progressBar_cycle = findViewById(R.id.chat_message_progressBar_cyclic);
                        progressBar_cycle.setVisibility(View.VISIBLE);

                        QBMessageUpdateBuilder messageUpdateBuilder = new QBMessageUpdateBuilder();
                        messageUpdateBuilder.updateText(edtContent.getText().toString()).markDelivered().markRead();

                        QBRestChatService.updateMessage(edtMessage.getId(), qbChatDialog.getDialogId(), messageUpdateBuilder)
                                .performAsync(new QBEntityCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid, Bundle bundle) {
                                        //Refresh data
                                        retrieveMessage();
                                        isEditMode = false;// reset variable

                                        //mDialog.dismiss();
                                        progressBar_cycle.setVisibility(View.GONE);

                                        //Reset edit text
                                        edtContent.setText("");
                                        edtContent.setFocusable(true);
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        Toast.makeText(getBaseContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        //系統會顯示你不能修改這段對話
                                        progressBar_cycle.setVisibility(View.GONE);
                                    }
                                });
                    }
                }
            }
        });
    }

    private void retrieveMessage() {
        QBMessageGetBuilder messageGetBuilder = new QBMessageGetBuilder();
        messageGetBuilder.setLimit(500);// get limit 500 messages

        if(qbChatDialog != null){
            QBRestChatService.getDialogMessages(qbChatDialog,messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
                @Override
                public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {
                    //Put  messages to cache (save list messages to cache and refresh list view)
                    QBChatMessagesHolder.getInstance().putMessages(qbChatDialog.getDialogId(),qbChatMessages);

                    adapter = new ChatMessageAdapter(getBaseContext(),qbChatMessages);
                    lstChatMessages.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onError(QBResponseException e) {

                }
            });
        }
    }

    private void initChatDialogs() {
        qbChatDialog = (QBChatDialog)getIntent().getSerializableExtra(Common.DIALOG_EXTRA);

            //Part 10 get avatar from Server and set for dialog 影片13:07
            if (qbChatDialog.getPhoto() != null && !qbChatDialog.getPhoto().equals("null")) {
                QBContent.getFile(Integer.parseInt(qbChatDialog.getPhoto()))
                        .performAsync(new QBEntityCallback<QBFile>() {
                            @Override
                            public void onSuccess(QBFile qbFile, Bundle bundle) {
                                String fileURL = qbFile.getPublicUrl();
                                Picasso.with(getBaseContext())
                                        .load(fileURL)
                                        .resize(50, 50)
                                        .centerCrop()
                                        .into(dialog_avatar);
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                Log.e("ERROR_IMAGE", "" + e.getMessage());
                            }
                        });
        }

        qbChatDialog.initForChat(QBChatService.getInstance());

        //Register listener to process imcoming message (建立監聽處理即時訊息)
        QBIncomingMessagesManager incomingMessagesManager = QBChatService.getInstance().getIncomingMessagesManager();
        incomingMessagesManager.addDialogMessageListener(new QBChatDialogMessageListener() {
            @Override
            public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {

            }

            @Override
            public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

            }
        });

        //Add typing listener
        registerTypingForChatDialog(qbChatDialog);

        //Add Join group to enable group chat
        if(qbChatDialog.getType() == QBDialogType.PUBLIC_GROUP || qbChatDialog.getType() == QBDialogType.GROUP){
            DiscussionHistory discussionHistory = new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);

            qbChatDialog.join(discussionHistory, new QBEntityCallback() {
                @Override
                public void onSuccess(Object o, Bundle bundle) {

                }

                @Override
                public void onError(QBResponseException e) {
                    Log.e("ERROR",""+e.getMessage());
                }
            });

        }

        //Part 9 - Online User count
        QBChatDialogParticipantListener participantListener = new QBChatDialogParticipantListener() {
            @Override
            public void processPresence(String dialogId, QBPresence qbPresence) {
                if(dialogId == qbChatDialog.getDialogId()){
                    QBRestChatService.getChatDialogById(dialogId)
                            .performAsync(new QBEntityCallback<QBChatDialog>() {
                                @Override
                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                    //Get Online user
                                    try {
                                        Collection<Integer> onlineList = qbChatDialog.getOnlineUsers();
                                        TextDrawable.IBuilder builder = TextDrawable.builder()
                                                .beginConfig()
                                                .withBorder(4)
                                                .endConfig()
                                                .round();
                                        TextDrawable online = builder.build("", Color.RED);
                                        img_online_count.setImageDrawable(online);

                                        txt_online_count.setText(String.format("%d/%d online",onlineList.size(),qbChatDialog.getOccupants().size()));
                                    } catch (XMPPException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                }
                            });
                }
            }
        };

        qbChatDialog.addParticipantListener(participantListener);

        qbChatDialog.addMessageListener(this);

        //Set title for toolbar  設定聊天室名稱
        toolbar.setTitle(qbChatDialog.getName());
        //toolbar.setTitle("TeatChat  beta");
        setSupportActionBar(toolbar);
    }

    private void registerTypingForChatDialog(QBChatDialog qbChatDialog) {
        QBChatDialogTypingListener typingListener = new QBChatDialogTypingListener() {
            @Override
            public void processUserIsTyping(String dialogId, Integer integer) {
                if(dotloader.getVisibility() != View.VISIBLE)
                    dotloader.setVisibility(View.VISIBLE);
            }

            @Override
            public void processUserStopTyping(String dialogId, Integer integer) {
                if(dotloader.getVisibility() != View.INVISIBLE)
                    dotloader.setVisibility(View.INVISIBLE);
            }
        };
        qbChatDialog.addIsTypingListener(typingListener);
    }

    private void initViews() {

        lstChatMessages = (ListView)findViewById(R.id.list_of_message);
        submitButton = (ImageButton)findViewById(R.id.send_button);
        edtContent = (EditText)findViewById(R.id.edt_content);

        //Part 9
        img_online_count = (ImageView)findViewById(R.id.img_online_count);
        txt_online_count = (TextView)findViewById(R.id.txt_online_count);

        //Part 10 Dialog Avatar
        dialog_avatar = (ImageView)findViewById(R.id.dialog_avatar);
        dialog_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChatMessageActivity.this, "Select one picture", Toast.LENGTH_SHORT).show();
                Intent selectImage = new Intent();
                selectImage.setType("image/*");
                selectImage.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(selectImage,"Select Picture"),Common.SELECT_PICTURE);
            }
        });

        //Part 12
        dotloader = (DotLoader)findViewById(R.id.dot_loader);
        edtContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                try {
                    qbChatDialog.sendIsTypingNotification();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //After tet changed, we will send stop typing notification
                try {
                    qbChatDialog.sendStopTypingNotification();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        });

        //Add contextmenu
        registerForContextMenu(lstChatMessages);

        //Add Toolbar
        toolbar = (Toolbar)findViewById(R.id.chat_message_toolbar);

        //Add context menu
        registerForContextMenu(lstChatMessages);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK)
        {
            if(requestCode == Common.SELECT_PICTURE)
            {
                Uri selectedImageUri = data.getData();
                //ProgressBar
                final ProgressBar progressBar_cycle;
                progressBar_cycle = findViewById(R.id.chat_message_progressBar_cyclic);
                progressBar_cycle.setVisibility(View.VISIBLE);
                try {
                    //Convert uri to file
                    InputStream in = getContentResolver().openInputStream(selectedImageUri);
                    final Bitmap bitmap = BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                    File file = new File(Environment.getExternalStorageDirectory()+"/image.png");
                    FileOutputStream fileOut = new FileOutputStream(file);
                    fileOut.write(bos.toByteArray());
                    fileOut.flush();
                    fileOut.close();

                    int imageSizeKb = (int)file.length()/1024;
                    if (imageSizeKb >= (1024*100))
                    {
                        Toast.makeText(this, "ERROR size", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //Upload file
                    QBContent.uploadFileTask(file,true,null)
                            .performAsync(new QBEntityCallback<QBFile>() {
                                @Override
                                public void onSuccess(QBFile qbFile, Bundle bundle) {
                                    qbChatDialog.setPhoto(qbFile.getId().toString());

                                    //Update Chat Dialog
                                    QBRequestUpdateBuilder requestUpdateBuilder = new QBRequestUpdateBuilder();
                                    QBRestChatService.updateGroupChatDialog(qbChatDialog,requestUpdateBuilder)
                                            .performAsync(new QBEntityCallback<QBChatDialog>() {
                                                @Override
                                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                                    //mDialog.dismiss();
                                                    progressBar_cycle.setVisibility(View.GONE);
                                                    dialog_avatar.setImageBitmap(bitmap);
                                                }

                                                @Override
                                                public void onError(QBResponseException e) {
                                                    Toast.makeText(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                }
                            });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        //Cache Message (Save message to cache and  refresh list view)
        QBChatMessagesHolder.getInstance().putMessage(qbChatMessage.getDialogId(),qbChatMessage);
        ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatMessage.getDialogId());
        adapter = new ChatMessageAdapter(getBaseContext(),messages);
        lstChatMessages.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {
        Log.e("ERROR",""+e.getMessage());
    }
}
