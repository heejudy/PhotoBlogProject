
package com.example.photoviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private EditText editSearch;
    private Spinner spinnerSort;
    private Spinner spinnerViewMode;
    private RecyclerView recyclerView;

    private final String site_url =
            "https://heeju1223.pythonanywhere.com";

    private final String token = BuildConfig.DJANGO_API_TOKEN;

    private CloadImage taskDownload;

    private final List<PhotoPost> allPosts = new ArrayList<>();

    private static class PhotoPost {

        Bitmap image;
        String title;
        String text;
        String createdDate;

        PhotoPost(Bitmap image, String title,
                  String text, String createdDate) {

            this.image = image;
            this.title = title;
            this.text = text;
            this.createdDate = createdDate;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        editSearch = findViewById(R.id.editSearch);
        spinnerSort = findViewById(R.id.spinnerSort);
        spinnerViewMode = findViewById(R.id.spinnerViewMode);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        String[] sortOptions = {
                "최신순", "오래된순"
        };

        ArrayAdapter<String> sortAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sortOptions
                );

        sortAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        updatePostList();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        String[] viewOptions = {
                "제목형", "목록형", "이미지형"
        };

        ArrayAdapter<String> viewAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        viewOptions
                );

        viewAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerViewMode.setAdapter(viewAdapter);

        spinnerViewMode.setSelection(
                ImageAdapter.MODE_IMAGE
        );

        spinnerViewMode.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        updatePostList();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                }
        );

        editSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                        updatePostList();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );
    }

    public void onClickDownload(View v) {

        if (taskDownload != null
                && taskDownload.getStatus()
                == AsyncTask.Status.RUNNING) {

            taskDownload.cancel(true);
        }

        textView.setText("서버에서 이미지 다운로드 중...");

        taskDownload = new CloadImage();

        taskDownload.execute(
                site_url + "/api_root/Post/"
        );
    }

    public void onClickUpload(View v) {

        Intent intent =
                new Intent(this, UploadActivity.class);

        startActivity(intent);
    }

    private void updatePostList() {

        String keyword = editSearch.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        List<PhotoPost> filteredPosts = new ArrayList<>();

        for (PhotoPost post : allPosts) {

            String title =
                    post.title.toLowerCase(Locale.ROOT);

            String content =
                    post.text.toLowerCase(Locale.ROOT);

            if (title.contains(keyword)
                    || content.contains(keyword)) {

                filteredPosts.add(post);
            }
        }

        Collections.sort(
                filteredPosts,
                (post1, post2) -> {

                    if (spinnerSort.getSelectedItemPosition() == 0) {

                        return post2.createdDate.compareTo(
                                post1.createdDate
                        );

                    } else {

                        return post1.createdDate.compareTo(
                                post2.createdDate
                        );
                    }
                }
        );

        List<ImageAdapter.ImageItem> displayItems =
                new ArrayList<>();

        for (PhotoPost post : filteredPosts) {

            displayItems.add(
                    new ImageAdapter.ImageItem(
                            post.image,
                            post.title,
                            post.text,
                            post.createdDate
                    )
            );
        }

        int selectedMode =
                spinnerViewMode.getSelectedItemPosition();

        if (selectedMode < 0) {
            selectedMode = ImageAdapter.MODE_IMAGE;
        }

        recyclerView.setAdapter(
                new ImageAdapter(displayItems, selectedMode)
        );

        textView.setText(
                "게시물: " + filteredPosts.size() + "개"
        );
    }

    private class CloadImage
            extends AsyncTask<String, Integer, List<PhotoPost>> {

        private String errorMessage = null;

        @Override
        protected List<PhotoPost> doInBackground(
                String... urls) {

            List<PhotoPost> postList = new ArrayList<>();
            HttpURLConnection conn = null;

            try {

                URL urlAPI = new URL(urls[0]);

                conn = (HttpURLConnection)
                        urlAPI.openConnection();

                conn.setRequestMethod("GET");

                conn.setRequestProperty(
                        "Authorization",
                        "Token " + token
                );

                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();

                Log.d(
                        "PhotoViewer",
                        "API 응답 코드: " + responseCode
                );

                if (responseCode
                        == HttpURLConnection.HTTP_OK) {

                    StringBuilder result = new StringBuilder();

                    try (InputStream is = conn.getInputStream();
                         BufferedReader reader =
                                 new BufferedReader(
                                         new InputStreamReader(is))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            result.append(line);
                        }
                    }

                    JSONArray aryJson =
                            new JSONArray(result.toString());

                    for (int i = 0;
                         i < aryJson.length();
                         i++) {

                        if (isCancelled()) {
                            break;
                        }

                        JSONObject postJson =
                                aryJson.getJSONObject(i);

                        String imageUrl =
                                postJson.optString("image", "");

                        if (imageUrl.isEmpty()
                                || imageUrl.equals("null")) {
                            continue;
                        }

                        URL originalUrl = new URL(
                                new URL(site_url),
                                imageUrl
                        );

                        String downloadUrl =
                                site_url + originalUrl.getFile();

                        HttpURLConnection imageConn = null;

                        try {

                            imageConn =
                                    (HttpURLConnection)
                                            new URL(downloadUrl)
                                                    .openConnection();

                            imageConn.setConnectTimeout(10000);
                            imageConn.setReadTimeout(10000);

                            imageConn.setRequestProperty(
                                    "Authorization",
                                    "Token " + token
                            );

                            try (InputStream imgStream =
                                         imageConn.getInputStream()) {

                                Bitmap bitmap =
                                        BitmapFactory.decodeStream(
                                                imgStream
                                        );

                                if (bitmap != null) {

                                    postList.add(
                                            new PhotoPost(
                                                    bitmap,
                                                    postJson.optString(
                                                            "title", ""
                                                    ),
                                                    postJson.optString(
                                                            "text", ""
                                                    ),
                                                    postJson.optString(
                                                            "created_date", ""
                                                    )
                                            )
                                    );
                                }
                            }

                        } catch (IOException e) {

                            Log.e(
                                    "PhotoViewer",
                                    "이미지 다운로드 오류",
                                    e
                            );

                        } finally {

                            if (imageConn != null) {
                                imageConn.disconnect();
                            }
                        }
                    }

                } else {

                    errorMessage =
                            "API 오류: HTTP " + responseCode;
                }

            } catch (IOException | JSONException e) {

                errorMessage = e.getMessage();

                Log.e(
                        "PhotoViewer",
                        "서버 연결 또는 JSON 처리 오류",
                        e
                );

            } finally {

                if (conn != null) {
                    conn.disconnect();
                }
            }

            return postList;
        }

        @Override
        protected void onPostExecute(
                List<PhotoPost> posts) {

            if (errorMessage != null) {

                textView.setText(
                        "동기화 실패: " + errorMessage
                );
                return;
            }

            allPosts.clear();
            allPosts.addAll(posts);

            updatePostList();

            Toast.makeText(
                    MainActivity.this,
                    "동기화 완료!",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
