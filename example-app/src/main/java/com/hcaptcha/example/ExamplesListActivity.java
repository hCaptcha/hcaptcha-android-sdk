package com.hcaptcha.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExamplesListActivity extends AppCompatActivity {

    private static class ExampleInfo {
        final Class<? extends Activity> activityClass;
        final String title;
        final String subTitle;

        public ExampleInfo(Class<? extends Activity> activityClass, String title, String subTitle) {
            this.activityClass = activityClass;
            this.title = title;
            this.subTitle = subTitle;
        }
    }

    private static final ExampleInfo[] EXAMPLES = {
            new ExampleInfo(ComposeActivity.class, "JetPack Compose Example", "Example of integration HCaptcha SDK in JetPack Compose application"),
            new ExampleInfo(MainActivity.class, "Test Panel", "Manual testing")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<HashMap<String, String>> data = new ArrayList<>();
        for (ExampleInfo exampleInfo : EXAMPLES) {
            HashMap<String, String> map = new HashMap<>();
            map.put("title", exampleInfo.title);
            map.put("subtitle", exampleInfo.subTitle);
            data.add(map);
        }

        String[] from = {"title", "subtitle"};
        int[] to = {android.R.id.text1, android.R.id.text2};
        SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_2, from, to);

        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            ExampleInfo example = EXAMPLES[position];
            Intent intent = new Intent(ExamplesListActivity.this, example.activityClass);
            startActivity(intent);
        });

        setContentView(listView);
    }
}
