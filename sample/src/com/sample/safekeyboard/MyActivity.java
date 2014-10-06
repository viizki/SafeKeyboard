package com.sample.safekeyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.feitian.safekeyboard.SafeKeyboard;

public class MyActivity extends Activity implements OnClickListener {

    private EditText et2, et3;
    private Button btn2, btn3, btnDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        et2 = (EditText)findViewById(R.id.et2);    // 带密码键盘的edit
        et3 = (EditText)findViewById(R.id.et3);    // 带密码键盘的edit
        
        btn2 = (Button)findViewById(R.id.btn2);
        btn3 = (Button)findViewById(R.id.btn3);
        btnDialog = (Button)findViewById(R.id.btn_dialog);

        // 构造函数。最大长度为30，输入类型为字母和数字
        new SafeKeyboard(et2);
        
        // 构造函数。自定义最大长度、输入类型
        new SafeKeyboard(et3, 10, SafeKeyboard.TYPE_DIGIT_ONLY);
        
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btnDialog.setOnClickListener(this);
    }
    
    /**
     *  从EditText的Tag中取出加密后的密文。
     *  （这里默认不使用加密，所以取出的Tag就是明文的String对象）
     */
    public String getSafeText(EditText editText) {
    	Object obj = editText.getTag(SafeKeyboard.TAG_KEY);
    	return obj == null ? "" :(String)obj;
    }

    public void alertDlg() {
    	EditText edit = new EditText(this);
    	AlertDialog.Builder db = new AlertDialog.Builder(this);
    	db.setView(edit);
    	SafeKeyboard kb = new SafeKeyboard(edit);
    	kb.setViewToBeShownIn(findViewById(android.R.id.content));
    	db.create().show();
    }
    
	@Override
	public void onClick(View v) {
		if (v == btn2) {
			Toast.makeText(MyActivity.this, getSafeText(et2), Toast.LENGTH_SHORT).show();
		} else if (v == btn3) {
			Toast.makeText(MyActivity.this, getSafeText(et3), Toast.LENGTH_SHORT).show();
		} else if (v == btnDialog) {
			alertDlg();
		}
	}
}
