package com.mvp.sarah;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageDisplayActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageView imageView = new ImageView(this);
        setContentView(imageView);

        Uri imageUri = getIntent().getData();
        if (imageUri != null) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(imageUri.getPath()));
        }
    }
} 