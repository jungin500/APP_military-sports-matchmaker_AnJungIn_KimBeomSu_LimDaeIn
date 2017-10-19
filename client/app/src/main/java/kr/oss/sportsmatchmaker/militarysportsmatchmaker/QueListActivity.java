package kr.oss.sportsmatchmaker.militarysportsmatchmaker;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.ResponseHandlerInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;

public class QueListActivity extends AppCompatActivity {

    private SessionManager smgr;
    private Proxy proxy;

    private TextView textQStatus;
    private Button quitMatchButton;
    private Button acceptMatchButton;
    private Button rejectMatchButton;

    private ArrayList<ListData2> QueDataArray;
    private boolean[] Que_Yes_or_No = {true,false,true,true,false};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_que_list);

        smgr = new SessionManager(getApplicationContext());
        proxy = new Proxy(getApplicationContext());

        final String id = smgr.getProfile().get(SessionManager.ID);
        final String name = smgr.getProfile().get(SessionManager.NAME);
        final String rank = smgr.getProfile().get(SessionManager.RANK);
        final String rankname = rank + " " + name;

        textQStatus = (TextView) findViewById(R.id.text_status);
        quitMatchButton = (Button) findViewById(R.id.quitMatch);
        acceptMatchButton = (Button) findViewById(R.id.acceptMatch);
        rejectMatchButton = (Button) findViewById(R.id.rejectMatch);

        QueDataArray = new ArrayList<ListData2>();
        ListView listview = (ListView) findViewById(R.id.quelist);
        final CustomAdapter2 customAdapter = new CustomAdapter2(this, R.layout.list_btn_sty, QueDataArray);
        listview.setAdapter(customAdapter);

        proxy.getUserInfo(new JsonHttpResponseHandler(){
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    final String match_status = response.getString("match_status");
                    // if not in match and waiting for match
                    if (match_status.equals("ready")){
                        smgr.changeMatchStatus(false, null);
                        smgr.changeStadiumName(null);

                        textQStatus.setText("현재 대기중인 시합이 없습니다. 찾아보세요!");
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) quitMatchButton.getLayoutParams();
                        params.weight = 0f;
                        quitMatchButton.setLayoutParams(params);
                    }
                    // if match, get match and....
                    else {
                        proxy.getUserMatch(new JsonHttpResponseHandler(){
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                try {
                                    boolean success = response.getBoolean("result");
                                    if (success) {
                                        JSONObject match = response.getJSONObject("match");
                                        // set match status and match id on session manager.
                                        smgr.changeMatchStatus(true, match.getString("matchId"));
                                        smgr.changeStadiumName(match.getString("stadium"));

                                        String gameTypeEng = match.getString("activityType");
                                        String gameTypeKor = "족구";
                                        if (gameTypeEng.equals("football"))
                                            gameTypeKor = "축구";
                                        else if (gameTypeEng.equals("basketball"))
                                            gameTypeKor = "농구";

                                        // i am initiator <=> show quit button.
                                        // TODO: disable button
                                        final String initiatorId = match.getString("initiatorId");
                                        if (id.equals(initiatorId)) {
                                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) quitMatchButton.getLayoutParams();
                                            params.weight = 0.25f;
                                            quitMatchButton.setLayoutParams(params);
                                        }
                                        else {
                                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) quitMatchButton.getLayoutParams();
                                            params.weight = 0.0f;
                                            quitMatchButton.setLayoutParams(params);
                                        }
                                        // i am pending <=> show accept/reject button.
                                        if (match_status.equals("pending")){
                                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) acceptMatchButton.getLayoutParams();
                                            params.weight = 0.2f;
                                            acceptMatchButton.setLayoutParams(params);
                                            LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) rejectMatchButton.getLayoutParams();
                                            params.weight = 0.2f;
                                            rejectMatchButton.setLayoutParams(params);
                                        }
                                        else{
                                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) acceptMatchButton.getLayoutParams();
                                            params.weight = 0.0f;
                                            acceptMatchButton.setLayoutParams(params);
                                            LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) rejectMatchButton.getLayoutParams();
                                            params.weight = 0.0f;
                                            rejectMatchButton.setLayoutParams(params);
                                        }
                                        JSONArray acceptPlayers = match.getJSONArray("players");
                                        JSONArray pendingPlayers = match.getJSONArray("pendingPlayers");
                                        JSONArray rejectedPlayers = match.getJSONArray("rejectedPlayers");
                                        final int accnum = acceptPlayers.length();
                                        final int pendnum = pendingPlayers.length();
                                        final int rejnum = rejectedPlayers.length();
                                        textQStatus.setText("현재 큐 정보입니다.");

                                        int anoncount = 0;
                                        final ArrayList<String> players = new ArrayList<String>();
                                        for (int i = 0; i < accnum; i++){
                                            String player = acceptPlayers.get(i).toString();
                                            if (player.equals("anon")) {
                                                anoncount++;
                                            }
                                            else {
                                                players.add(player);
                                            }
                                        }
                                        for (int i = 0; i < pendnum; i++){
                                            players.add(pendingPlayers.get(i).toString());
                                        }
                                        for (int i = 0; i < rejnum; i++){
                                            players.add(rejectedPlayers.get(i).toString());
                                        }
                                        final String[] pArray = players.toArray(new String[players.size()]);
                                        final int finalAnoncount = anoncount;
                                        proxy.getUsersDetails(pArray, new JsonHttpResponseHandler(){
                                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                                try {
                                                    if (response.getBoolean("result")){
                                                        if (!response.getBoolean("complete")){
                                                            Log.e("TAG", "data corrupt: complete gives false on existing ids");
                                                        }
                                                        JSONArray userData = response.getJSONArray("data");
                                                        String initrankname = initiatorId;
                                                        for (int i=0; i < pArray.length; i++){
                                                            String currid = userData.getJSONObject(i).getString("id");
                                                            String currrankname = RankHelper.intToRank(userData.getJSONObject(i).getInt("rank")) + " " + userData.getJSONObject(i).getString("name");
                                                            boolean existProfPic = userData.getJSONObject(i).getBoolean("profile_image");
                                                            String userStatus = userData.getJSONObject(i).getString("match_status");
                                                            String buttonInput = "수락함";
                                                            if (currid.equals(initiatorId)) {
                                                                buttonInput = "방장";
                                                                initrankname = currrankname;
                                                            }
                                                            else if (userStatus.equals("pending")) buttonInput = "대기중";
                                                            else if (userStatus.equals("ready")) buttonInput = "거절함";
                                                            ListData2 listData = new ListData2(existProfPic, currrankname, currid, buttonInput);
                                                            QueDataArray.add(listData);
                                                        }
                                                        for (int j=0;j<finalAnoncount;j++){
                                                            String anonName = initrankname + " 의 동료";
                                                            ListData2 listData = new ListData2(false, anonName, "anon", "수락함");
                                                            QueDataArray.add(listData);
                                                        }
                                                        Collections.sort(QueDataArray, new ListData2.data2Comparator());
                                                        customAdapter.notifyDataSetChanged();
                                                    }
                                                    else {
                                                        Log.e("TAG", "getUsersDetails failed");
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                    else {
                                        Log.e("TAG", "DATA CORRUPT; user status not *ready* but no match");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Log.e("TAG", "JSON error");
                                }
                            }
                            @Override
                            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                textQStatus.setText("매치 정보를 가져오지 못했습니다. 다시 접속해주세요.");
                            }

                            @Override
                            public void onPostProcessResponse(ResponseHandlerInterface instance, HttpResponse response) {
                                super.onPostProcessResponse(instance, response);
                            }
                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


        // set quitMatch listener
        quitMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // do nothing if not in match
                if (!smgr.getMatchStatus()){
                    return;
                }
                String matchId = smgr.getMatchId();

                proxy.deleteMatch(matchId, new JsonHttpResponseHandler(){
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            if(response.getBoolean("result")){
                                smgr.changeMatchStatus(false, null);
                                smgr.changeStadiumName(null);
                                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else{
                                String reason = response.getString("reason");
                                if (reason.equals("ForbiddenOperationException")){
                                    Toast.makeText(getApplicationContext(), "삭제 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                                }
                                else if (reason.equals("NoSuchMatchException")){
                                    Toast.makeText(getApplicationContext(), "진행중인 큐가 없습니다..", Toast.LENGTH_SHORT).show();
                                }
                                else if (reason.equals("NotLoggedInException")){
                                    Toast.makeText(getApplicationContext(), "로그인되어있지 않습니다.", Toast.LENGTH_SHORT).show();
                                    smgr.checkSession();
                                }
                                else {
                                    Toast.makeText(getApplicationContext(), "실패했습니다. 오류 종류: " + reason, Toast.LENGTH_SHORT).show();
                                    Log.e("deleteMatch error", reason);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        // 수락 혹은 거절 버튼 누를시 하는 일
        acceptMatchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proxy.decideMatch("true", new JsonHttpResponseHandler(){
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        for (ListData2 i : QueDataArray){
                            if (id.equals(i.getId())){
                                i.setButton("수락함");
                            }
                        }
                        // sort dataset.
                        Collections.sort(QueDataArray, new ListData2.data2Comparator());


                        // remove 수락/거절 버튼
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) acceptMatchButton.getLayoutParams();
                        params.weight = 0f;
                        acceptMatchButton.setLayoutParams(params);
                        LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) rejectMatchButton.getLayoutParams();
                        params.weight = 0f;
                        rejectMatchButton.setLayoutParams(params);

                        customAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        //reject Match 버튼 구현
        rejectMatchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proxy.decideMatch("", new JsonHttpResponseHandler(){
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        for (ListData2 i : QueDataArray){
                            if (id.equals(i.getId())){
                                i.setButton("거절함");
                            }
                        }
                        customAdapter.notifyDataSetChanged();
                        smgr.changeMatchStatus(false, null);
                        smgr.changeStadiumName(null);
                        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });





        customAdapter.notifyDataSetChanged();
    }
}