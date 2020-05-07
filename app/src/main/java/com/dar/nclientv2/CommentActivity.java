package com.dar.nclientv2;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.dar.nclientv2.adapters.CommentAdapter;
import com.dar.nclientv2.api.components.Comment;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.components.activities.BaseActivity;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CommentActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.initActivity(this);
        setContentView(R.layout.activity_comment);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.comments);
        findViewById(R.id.page_switcher).setVisibility(View.GONE);
        Gallery g=getIntent().getParcelableExtra(getPackageName()+".GALLERY");
        recycler=findViewById(R.id.recycler);
        refresher=findViewById(R.id.refresher);
        findViewById(R.id.card).setVisibility(Login.isLogged()?View.VISIBLE:View.GONE);
        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringWriter writer=new StringWriter();
                JsonWriter writer1=new JsonWriter(writer);
                if(((EditText)findViewById(R.id.commentText)).getText().toString().length()<10)return;
                try {
                    writer1.beginObject()
                            .name("body").value(((EditText)findViewById(R.id.commentText)).getText().toString())
                            .name("gallery_id").value(g.getId())
                            .endObject();
                    writer1.flush();
                    writer1.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),writer.toString());
                assert Global.getClient()!=null;
                Global.getClient().newCall(new Request.Builder().url(Utility.getBaseUrl()+"g/"+g.getId()).build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String token=response.body().string();
                        token=token.substring(token.lastIndexOf("csrf_token"));
                        token=token.substring(token.indexOf('"')+1);
                        token=token.substring(0,token.indexOf('"'));
                        Global.getClient().newCall(new Request.Builder().addHeader("Referer","https://"+ Utility.getHost()+"/g/"+g.getId()).addHeader("X-Requested-With","XMLHttpRequest").addHeader("X-CSRFToken",token).post(body).url(Utility.getBaseUrl()+"api/comments/submit").build()).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {

                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String s=response.body().string();
                                LogUtility.d(s);
                                //JsonReader reader =new JsonReader(response.body().charStream());
                                JsonReader reader =new JsonReader(new StringReader(s));
                                boolean success=false;
                                Comment c=null;
                                reader.beginObject();
                                while(reader.peek()!= JsonToken.END_OBJECT){
                                    switch (reader.nextName()){
                                        case "success":success=reader.nextBoolean();break;
                                        case "comment":c=new Comment(reader,false);break;
                                        default:reader.skipValue();
                                    }
                                }
                                reader.close();
                                if(success){
                                    ((CommentAdapter)recycler.getAdapter()).addComment(c);
                                    CommentActivity.this.runOnUiThread(() -> {
                                        ((EditText)findViewById(R.id.commentText)).setText("");
                                        recycler.smoothScrollToPosition(0);
                                    });

                                }
                            }
                        });
                    }
                });




            }
        });
        changeLayout(getResources().getConfiguration().orientation== Configuration.ORIENTATION_LANDSCAPE);
        refresher.setRefreshing(true);
        recycler.setAdapter(new CommentAdapter(this,g.getComments(),g.getId()));
        recycler.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        refresher.setRefreshing(false);
        refresher.setEnabled(false);
    }

    @Override
    protected int getPortCount() {
        return 1;
    }

    @Override
    protected int getLandCount() {
        return 2;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home: onBackPressed();return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
