package com.example.drinkzlyboozeserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.drinkzlyboozeserver.view_holder.ChatPictureHolder;
import com.example.drinkzlyboozeserver.view_holder.ChatTextHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import callback.ILoadTimeFromFirebaseListener;
import common.Common;
import model.ChatInfoModel;
import model.ChatMessageModel;

public class ChatDetailActivity extends AppCompatActivity implements ILoadTimeFromFirebaseListener {
    private static final int MY_CAMERA_REQUEST_CODE=7171;
    private static final int MY_RESULT_LOAD_IMG = 7172;

    private String roomId,chatSender;


    @BindView(R.id.toolbar1)
    Toolbar toolbar;
    @BindView(R.id.img_camera)
    ImageView img_camera;
    @BindView(R.id.img_image)
    ImageView img_image;
    @BindView(R.id.edt_chat)
    AppCompatEditText edt_chat;
    @BindView(R.id.img_send)
    ImageView img_send;
    @BindView(R.id.recycler_chat)
    RecyclerView recycler_chat;
    @BindView(R.id.img_preview)
    ImageView img_preview;

    FirebaseDatabase database;
    DatabaseReference chatRef,offsetRef;
    ILoadTimeFromFirebaseListener listener;

    FirebaseRecyclerAdapter<ChatMessageModel,RecyclerView.ViewHolder> adapter;
    FirebaseRecyclerOptions<ChatMessageModel> options;

    Uri fileUri;

    StorageReference storageReference;

    LinearLayoutManager layoutManager;

    @OnClick(R.id.img_image)
    void onSelectImageClick(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,MY_RESULT_LOAD_IMG);
    }

    @OnClick(R.id.img_camera)
    void onCaptureImageClick(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        fileUri = getOutputMediaFileUri();
        intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);
        startActivityForResult(intent,MY_CAMERA_REQUEST_CODE);
    }

    @OnClick(R.id.img_send)
    void onSubmitChatClick(){
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long offset = snapshot.getValue(Long.class);
                long estimatedServerTimeInMs = System.currentTimeMillis() + offset;

                listener.onLoadOnlyTimeSuccess(estimatedServerTimeInMs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onLoadTimeFailed(error.getMessage());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        initViews();

        loadChatContent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(adapter!=null)
            adapter.startListening();
    }

    @Override
    protected void onStop() {
        if(adapter!=null)
            adapter.stopListening();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(adapter!=null)
            adapter.startListening();
    }

    private void loadChatContent() {
        adapter = new FirebaseRecyclerAdapter<ChatMessageModel, RecyclerView.ViewHolder>(options) {

            @Override
            public int getItemViewType(int position) {
                return adapter.getItem(position).isPicture() ? 1 : 0;
            }

            @Override
            protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull ChatMessageModel model) {
                if(holder instanceof ChatTextHolder)
                {
                    ChatTextHolder chatTextHolder = (ChatTextHolder)holder;
                    chatTextHolder.txt_email.setText(model.getName());
                    chatTextHolder.txt_chat_message.setText(model.getContent());
                    chatTextHolder.txt_time.setText(
                            DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                                    Calendar.getInstance().getTimeInMillis(),0).toString());


                }
                else
                {
                    ChatPictureHolder chatPictureHolder = (ChatPictureHolder)holder;
                    chatPictureHolder.txt_email.setText(model.getName());
                    chatPictureHolder.txt_chat_message.setText(model.getContent());
                    chatPictureHolder.txt_time.setText(
                            DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                                    Calendar.getInstance().getTimeInMillis(),0).toString());
                    Glide.with(ChatDetailActivity.this)
                            .load(model.getPictureLink())
                            .into(chatPictureHolder.img_preview);

                }
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view;
                if(viewType==0)
                {
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_message_text,parent,false);
                    return new ChatTextHolder(view);
                }
                else
                {
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_message_picture,parent,false);
                    return new ChatPictureHolder(view);
                }
            }
        };
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = adapter.getItemCount();
                int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
                if(lastVisiblePosition== -1 ||
                        (positionStart >= (friendlyMessageCount-1) &&
                                lastVisiblePosition == (positionStart-1))){
                    recycler_chat.scrollToPosition(positionStart);

                }
            }
        });

        recycler_chat.setAdapter(adapter);
    }

    private void initViews() {

        roomId = getIntent().getStringExtra(Common.KEY_ROOM_ID);
        chatSender = getIntent().getStringExtra(Common.KEY_CHAT_USER);

        listener = this;
        database = FirebaseDatabase.getInstance();
        chatRef = database.getReference(Common.BRANCH_REF)
                .child(Common.currentServerUser.getBranch())
                .child(Common.CHAT_REF);
        offsetRef = database.getReference(".info/serverTimeOffset");

        Query query = chatRef.child(roomId)
                .child(Common.CHAT_DETAIL_REF);

        options = new FirebaseRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query,ChatMessageModel.class)
                .build();

        ButterKnife.bind(this);

        layoutManager = new LinearLayoutManager(this);
        recycler_chat.setLayoutManager(layoutManager);

        toolbar.setTitle(chatSender);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });

    }

    private Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "DrinkzlyBooze");
        if(!mediaStorageDir.exists())
        {
            if(!mediaStorageDir.mkdir())
                return null;
        }

        String time_stamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile = new File(new StringBuilder(mediaStorageDir.getPath())
                .append(File.separator)
                .append("IMG_")
                .append(time_stamp)
                .append("_")
                .append(new Random().nextInt())
                .append(".jpg")
                .toString());

        return mediaFile;

    }



    @Override
    public void onLoadOnlyTimeSuccess(long estimateTimeInMs) {
        ChatMessageModel chatMessageModel = new ChatMessageModel();
        chatMessageModel.setName(Common.currentServerUser.getName());
        chatMessageModel.setContent(edt_chat.getText().toString());
        chatMessageModel.setTimeStamp(estimateTimeInMs);

        if(fileUri==null)
        {
            chatMessageModel.setPicture(false);
            submitChatToFirebase(chatMessageModel,chatMessageModel.isPicture(),estimateTimeInMs);
        }
        else
        {
            uploadPictures(fileUri,chatMessageModel,estimateTimeInMs);
        }
    }

    private void uploadPictures(Uri fileUri, ChatMessageModel chatMessageModel,long estimateTimeInMs) {
        if(fileUri!=null)
        {
            AlertDialog dialog = new AlertDialog.Builder(ChatDetailActivity.this)
                    .setCancelable(false)
                    .setMessage("Uploading...")
                    .create();
            dialog.show();

            String fileName = Common.getFileName(getContentResolver(),fileUri);
            String path = new StringBuilder(Common.currentServerUser.getBranch())
                    .append("/")
                    .append(fileName)
                    .toString();

            storageReference = FirebaseStorage.getInstance().getReference(path);

            UploadTask uploadTask = storageReference.putFile(fileUri);

            Task task = uploadTask.continueWithTask(task1 -> {
                if(!task1.isSuccessful())
                    Toast.makeText(this, "Failed to upload", Toast.LENGTH_SHORT).show();
                return storageReference.getDownloadUrl();
            }).addOnCompleteListener(task12 -> {
                if(task12.isSuccessful())
                {
                    String url = task12.getResult().toString();
                    dialog.dismiss();

                    chatMessageModel.setPicture(true);
                    chatMessageModel.setPictureLink(url);

                    submitChatToFirebase(chatMessageModel,chatMessageModel.isPicture(),estimateTimeInMs);

                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        }
        else
            Toast.makeText(this, "Image is empty!", Toast.LENGTH_SHORT).show();
    }

    private void submitChatToFirebase(ChatMessageModel chatMessageModel, boolean isPicture,long estimateTimeInMs) {
        chatRef.child(roomId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists())
                        {
                            appendChat(chatMessageModel,isPicture,estimateTimeInMs);
                        }
                        else
                        {
                            createChat(chatMessageModel,isPicture,estimateTimeInMs);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatDetailActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void appendChat(ChatMessageModel chatMessageModel, boolean isPicture, long estimateTimeInMs) {
        Map<String,Object> update_data = new HashMap<>();
        update_data.put("lastUpdate",estimateTimeInMs);
        if(isPicture)
            update_data.put("lastMessage","<Image>");
        else
            update_data.put("lastMessage",chatMessageModel.getContent());

        chatRef.child(roomId)
                .updateChildren(update_data)
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatDetailActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task2 ->{
            if(task2.isSuccessful())
            {
                chatRef.child(roomId)
                        .child(Common.CHAT_DETAIL_REF)
                        .push()
                        .setValue(chatMessageModel)
                        .addOnFailureListener(e -> Toast.makeText(ChatDetailActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show())
                        .addOnCompleteListener(task -> {
                            if(task.isSuccessful())
                            {
                                edt_chat.setText("");
                                edt_chat.requestFocus();
                                if(adapter!=null)
                                {
                                    adapter.notifyDataSetChanged();
                                    if(isPicture)
                                    {
                                        fileUri=null;
                                        img_preview.setVisibility(View.GONE);
                                    }
                                }
                            }
                        });
            }
        });

    }

    private void createChat(ChatMessageModel chatMessageModel, boolean isPicture, long estimateTimeInMs) {
        ChatInfoModel chatInfoModel = new ChatInfoModel();
        chatInfoModel.setCreateName(chatMessageModel.getName());
        if(isPicture)
            chatInfoModel.setLastMessage("<Image>");
        else
            chatInfoModel.setLastMessage(chatMessageModel.getContent());
        chatInfoModel.setLastUpdate(estimateTimeInMs);
        chatInfoModel.setCreateDate(estimateTimeInMs);

        chatRef.child(roomId)
                .setValue(chatInfoModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task2 -> {
                    if(task2.isSuccessful())
                    {
                        chatRef.child(roomId)
                                .child(Common.CHAT_DETAIL_REF)
                                .push()
                                .setValue(chatMessageModel)
                                .addOnFailureListener(e -> Toast.makeText(ChatDetailActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show())
                                .addOnCompleteListener(task -> {
                                    if(task.isSuccessful())
                                    {
                                        edt_chat.setText("");
                                        edt_chat.requestFocus();
                                        if(adapter!=null)
                                        {
                                            adapter.notifyDataSetChanged();
                                            if(isPicture)
                                            {
                                                fileUri=null;
                                                img_preview.setVisibility(View.GONE);
                                            }
                                        }
                                    }
                                });
                    }
                });
    }

    @Override
    public void onLoadTimeFailed(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == MY_CAMERA_REQUEST_CODE)
        {
            if(resultCode == RESULT_OK)
            {
                Bitmap bitmap = null;
                ExifInterface ei = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),fileUri);
                    ei = new ExifInterface(getContentResolver().openInputStream(fileUri));

                    int orientation = ei.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED);
                    Bitmap rotateBitmap = null;
                    switch (orientation)
                    {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotateBitmap = rotateBitmap(bitmap,90);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotateBitmap = rotateBitmap(bitmap,180);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotateBitmap = rotateBitmap(bitmap,270);
                            break;
                        default:
                            rotateBitmap = bitmap;
                            break;
                    }
                    img_preview.setVisibility(View.VISIBLE);
                    img_preview.setImageBitmap(rotateBitmap);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else if(requestCode == MY_RESULT_LOAD_IMG)
        {
            if(resultCode == RESULT_OK)
            {
                try {
                    final Uri imageUri = data.getData();
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap selectedImage = BitmapFactory.decodeStream(inputStream);
                    img_preview.setImageBitmap(selectedImage);
                    img_preview.setVisibility(View.VISIBLE);
                    fileUri = imageUri;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                }
            }
            else
            {
                Toast.makeText(this, "Please choose image", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private Bitmap rotateBitmap(Bitmap bitmap, int i)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(i);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,true);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}