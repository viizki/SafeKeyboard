package com.viizki.safekeyboard;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

import com.example.testKeyboard.R;


public class SafeKeyboard implements OnClickListener, OnKeyListener, OnDismissListener {

    public static final int TYPE_DIGIT_ONLY = 1;    // 只允许输入数字
    public static final int TYPE_DIGIT_LETTER = 2;  // 允许输入字母和数字
    
    public static final int TAG_KEY = 65248558;             // EditText.setTag(int key, Object obj) 中的key值
    
    private Context context;
    
    private boolean isLetterAllowed;   // 允许输入字母
    private boolean isDigitAllowed;    // 允许输入数字
    private boolean isLetterOrdered;   // 字母键盘有序
    private boolean isDigitOrdered;    // 数字键盘有序
    
    private boolean isCapLock;        // 当前是否是大写字母状态
    
    private StringBuffer mStrDisplay;   // 要显示的字符串（****）
    private String mStrEncrypt;         // 加密后的字符串
    private int maxLength;              // 最大长度
    
    private final char[] orderedLetters = new char[] {        // 有序的小写字母列表
            'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p',
            'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l',
            'z', 'x', 'c', 'v', 'b', 'n', 'm'
    };
    private final char[] orderedCapLetters = new char[] {     // 有序的大写字母列表
            'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',
            'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
            'Z', 'X', 'C', 'V', 'B', 'N', 'M'
    };
    private final char[] orderedDigits = new char[] {         // 有序的数字列表
            '1', '2', '3', '0', '4', '5', '6', '7', '8', '9'
    };
    
    private PopupWindow pop;
    
    private LinearLayout keyboard;
    private LinearLayout layoutLetter;
    private LinearLayout layoutDigit;

    private View mViewToBeShownIn;
    private EditText etTarget;        // 目标输入框
    private EditText etPsw;           // 键盘上 的密码框
    private Button btnOk;             // 键盘上的完成按钮
    
    private ImageButton btnLetterDel;
    private ImageButton btnLetterCap;
    private Button btnLetterSwitch;
    private Button btnLetters[];
    private char  letters[];
    
    Bitmap bmpCapNormal = null;
    Bitmap bmpCapLocked = null;
    
    private ImageButton btnDigitDel;
    private Button btnDigitSwitch;
    private Button btnDigits[];
    private char digits[];
    private ICrypto mCrypto;


    /**
     * 安全键盘要使用的加密算法接口。
     */
    public interface ICrypto {
        public String enc(String txt);
        public String dec(String txt, int len);
    }


    /**
     * 构造方法。（默认最大长度为30，输入类型为字母和数字，字母键有序，数字键随机打乱）
     * @param editText  要绑定安全键盘的EditText对象
     */
    public SafeKeyboard(EditText editText) {
        this(editText, 30, TYPE_DIGIT_LETTER);
    }

    /**
     * 构造方法。（默认输入类型为字母和数字，字母键有序，数字键随机打乱）
     * @param editText  要绑定安全键盘的EditText对象
     * @param maxLength 可输入的最大长度
     */
    public SafeKeyboard(EditText editText, int maxLength) {
        this(editText, maxLength, TYPE_DIGIT_LETTER);
    }
    
    /**
     * 构造方法。
     * @param editText  要绑定安全键盘的EditText对象
     * @param maxLength 可输入的最大长度
     * @param inputType 输入类型（TYPE_DIGIT_ONLY 或 TYPE_DIGIT_LETTER）
     */
    public SafeKeyboard(EditText editText, int maxLength, int inputType) {
        this(editText, maxLength, inputType, true, false);
    }

    /**
     * 构造方法。
     * @param editText   要绑定安全键盘的EditText对象
     * @param maxLength  可输入的最大长度
     * @param inputType  输入类型（TYPE_DIGIT_ONLY 或 TYPE_DIGIT_LETTER）
     * @param isLetterOrdered  字母键是否有序
     * @param isDigitOrdered   数字键是否有序
     */
    public SafeKeyboard(EditText editText, int maxLength, int inputType,
                        boolean isLetterOrdered, boolean isDigitOrdered) {

        if (editText == null) {
            throw new RuntimeException("Param editText can not be null.");
        }

        this.etTarget = editText;
        this.mViewToBeShownIn = etTarget;
        this.context = editText.getContext();

        initUI();

        this.setCryptTool(new DefaultCrypto());

//        reset();

        this.maxLength = maxLength > 30 ? 30 : maxLength;
        this.isLetterOrdered = isLetterOrdered;
        this.isDigitOrdered = isDigitOrdered;
        if (inputType == TYPE_DIGIT_ONLY) {
            isDigitAllowed = true;
            isLetterAllowed = false;
            initDigits();
        } else if (inputType == TYPE_DIGIT_LETTER) {
            isDigitAllowed = true;
            isLetterAllowed = true;
            initLetters();
        }

        bind(editText);

    }
    
    private void reset() {
    	this.mStrDisplay = new StringBuffer();
        this.mStrEncrypt = null;
        this.etPsw.setText("");
        this.isCapLock = false;
        this.etTarget.setText("");
    }
    
    private void initUI() {
        
    	
    	
//        LayoutInflater inf = LayoutInflater.from(context);
//        keyboard = (LinearLayout)inf.inflate(R.layout.keyboard, null);
//        
//        ViewGroup layoutTop = (ViewGroup)keyboard.getChildAt(0);
//        etPsw = (EditText)layoutTop.getChildAt(0);
//        btnOk = (Button)layoutTop.getChildAt(1);
        
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        
        Bitmap bmpBtnBg1 = null;
        Bitmap bmpBtnBg2 = null;
        Bitmap bmpDelete = null;
        
        AssetManager am = context.getAssets();
        try {
        	InputStream is1 = am.open("key_bg_1.9.png");
        	InputStream is2 = am.open("key_bg_2.9.png");
        	InputStream is3 = am.open("key_cap_normal.png");
        	InputStream is4 = am.open("key_cap_pressed.png");
        	InputStream is5 = am.open("key_del.png");
        	
        	bmpBtnBg1 = BitmapFactory.decodeStream(is1);
        	bmpBtnBg2 = BitmapFactory.decodeStream(is2);
        	bmpCapNormal = BitmapFactory.decodeStream(is3);
        	bmpCapLocked = BitmapFactory.decodeStream(is4);
        	bmpDelete = BitmapFactory.decodeStream(is5);
        	
        	is1.close();
        	is2.close();
        	is3.close();
        	is4.close();
        	is5.close();
        	
        } catch (IOException e) {
        	e.printStackTrace();
        }
        
        bmpBtnBg1.setDensity(metric.densityDpi);
        bmpBtnBg2.setDensity(metric.densityDpi);
        bmpCapLocked.setDensity(DisplayMetrics.DENSITY_HIGH);
        bmpCapNormal.setDensity(DisplayMetrics.DENSITY_HIGH);
        bmpDelete.setDensity(DisplayMetrics.DENSITY_HIGH);
        Drawable bgTransp = new ColorDrawable(Color.TRANSPARENT);
        
        ViewGroup.LayoutParams lp1 = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, (int)(60 * metric.density), 1);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(0, (int)(45 * metric.density), 1);
        LinearLayout.LayoutParams lp4 = new LinearLayout.LayoutParams((int)(65 * metric.density), (int)(45 * metric.density), 0);
        
        keyboard = new LinearLayout(context);
    	keyboard.setLayoutParams(lp1);
    	keyboard.setOrientation(LinearLayout.VERTICAL);
    	keyboard.setFocusable(true);
    	keyboard.setFocusableInTouchMode(true);
    	keyboard.setBackgroundColor(Color.rgb(0x3F, 0x3F, 0x3F));
    	
    	LinearLayout layoutNav = new LinearLayout(context);
    	layoutNav.setLayoutParams(lp1);
    	layoutNav.setOrientation(LinearLayout.HORIZONTAL);
    	layoutNav.setGravity(Gravity.CENTER_VERTICAL);
    	
    	etPsw = new EditText(context);
    	etPsw.setLayoutParams(lp3);
    	etPsw.setPadding(20, 0, 0, 0);
    	etPsw.setTextSize(16);
    	etPsw.setTextColor(Color.WHITE);
    	etPsw.setBackgroundDrawable(get9PatchDrawable(bmpBtnBg1));
        
    	btnOk = new Button(context);
    	btnOk.setLayoutParams(lp4);
    	btnOk.setTextSize(16);
    	btnOk.setTextColor(Color.WHITE);
    	btnOk.setText("OK");
    	btnOk.setTypeface(Typeface.DEFAULT_BOLD);
    	setBtnSelectorBackground(btnOk, get9PatchDrawable(bmpBtnBg1), bgTransp);
    	
		layoutNav.addView(etPsw);
		layoutNav.addView(btnOk);
		keyboard.addView(layoutNav);
    	
		System.out.println(metric.densityDpi + "," + metric.density);
		
        
		
//		etPsw.setBackgroundDrawable(get9PatchDrawable(bmpBtnBg1));
//		setBtnSelectorBackground(btnOk, get9PatchDrawable(bmpBtnBg1), bgTransp);
		
		
        /**  ================== 字母键盘  ================ **/
        
        layoutLetter = new LinearLayout(context);
        layoutLetter.setLayoutParams(lp1);
        layoutLetter.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout layoutRow1 = new LinearLayout(context);
        layoutRow1.setLayoutParams(lp1);
        layoutRow1.setOrientation(LinearLayout.HORIZONTAL);
        layoutRow1.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout layoutRow2 = new LinearLayout(context);
        layoutRow2.setLayoutParams(lp1);
        layoutRow2.setOrientation(LinearLayout.HORIZONTAL);
        layoutRow2.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout layoutRow3 = new LinearLayout(context);
        layoutRow3.setLayoutParams(lp1);
        layoutRow3.setOrientation(LinearLayout.HORIZONTAL);
        layoutRow3.setGravity(Gravity.CENTER_VERTICAL);
        
        btnLetters = new Button[26];
        for (int i = 0; i < 26; i++) {
            btnLetters[i] = new Button(context);
            btnLetters[i].setLayoutParams(lp2);
            btnLetters[i].setTypeface(Typeface.DEFAULT_BOLD);
            btnLetters[i].setTextSize(16);
            btnLetters[i].setTextColor(Color.WHITE);
            setBtnSelectorBackground(btnLetters[i], get9PatchDrawable(bmpBtnBg1), bgTransp);
        }
        
        btnLetterDel = new ImageButton(context);
        btnLetterDel.setLayoutParams(lp2);
        btnLetterDel.setPadding(0, 0, 0, 0);
        btnLetterDel.setImageBitmap(bmpDelete);
        setBtnSelectorBackground(btnLetterDel, get9PatchDrawable(bmpBtnBg2), bgTransp);
        
        btnLetterCap = new ImageButton(context);
        btnLetterCap.setLayoutParams(lp2);
        btnLetterCap.setPadding(0, 0, 0, 0);
        setBtnSelectorBackground(btnLetterCap, get9PatchDrawable(bmpBtnBg2), bgTransp);
        
        btnLetterSwitch = new Button(context);
        btnLetterSwitch.setLayoutParams(new LinearLayout.LayoutParams(0, (int)(60 * metric.density), 2));
        btnLetterSwitch.setText("123");
        btnLetterSwitch.setTextColor(Color.WHITE);
        btnLetterSwitch.setPadding(0, 0, 0, 0);
        btnLetterSwitch.setSingleLine(true);
        setBtnSelectorBackground(btnLetterSwitch, get9PatchDrawable(bmpBtnBg2), bgTransp);
        
        for (int i = 0; i < 10; i++)
            layoutRow1.addView(btnLetters[i]);
        
        for (int i = 10; i < 19; i++)
            layoutRow2.addView(btnLetters[i]);
        
        layoutRow2.addView(btnLetterDel);
        
        layoutRow3.addView(btnLetterCap);
        
        for (int i = 19; i < 26; i++)
            layoutRow3.addView(btnLetters[i]);
        
        layoutRow3.addView(btnLetterSwitch);
        
        layoutLetter.addView(layoutRow1);
        layoutLetter.addView(layoutRow2);
        layoutLetter.addView(layoutRow3);
        
        /**  ================== 数字键盘  ================ **/
        
        layoutDigit = new LinearLayout(context);
        layoutDigit.setLayoutParams(lp1);
        layoutDigit.setOrientation(LinearLayout.VERTICAL);
        
        layoutRow1 = new LinearLayout(context);
        layoutRow1.setLayoutParams(lp1);
        layoutRow1.setOrientation(LinearLayout.HORIZONTAL);
        layoutRow1.setGravity(Gravity.CENTER_VERTICAL);
        
        layoutRow2 = new LinearLayout(context);
        layoutRow2.setLayoutParams(lp1);
        layoutRow2.setOrientation(LinearLayout.HORIZONTAL);
        layoutRow2.setGravity(Gravity.CENTER_VERTICAL);
        
        layoutRow3 = new LinearLayout(context);
        layoutRow3.setLayoutParams(lp1);
        layoutRow3.setOrientation(LinearLayout.HORIZONTAL);
        layoutRow3.setGravity(Gravity.CENTER_VERTICAL);
        
        btnDigits = new Button[10];
        for (int i = 0; i < 10; i++) {
        	btnDigits[i] = new Button(context);
        	btnDigits[i].setLayoutParams(lp2);
        	btnDigits[i].setTypeface(Typeface.DEFAULT_BOLD);
        	btnDigits[i].setTextSize(20);
        	btnDigits[i].setTextColor(Color.WHITE);
            setBtnSelectorBackground(btnDigits[i], get9PatchDrawable(bmpBtnBg1), bgTransp);
        }
        
        btnDigitDel = new ImageButton(context);
        btnDigitDel.setLayoutParams(lp2);
        btnDigitDel.setPadding(0, 0, 0, 0);
        btnDigitDel.setImageBitmap(bmpDelete);
        setBtnSelectorBackground(btnDigitDel, get9PatchDrawable(bmpBtnBg2), bgTransp);
        
        btnDigitSwitch = new Button(context);
        btnDigitSwitch.setLayoutParams(lp2);
        btnDigitSwitch.setText("ABC");
        btnDigitSwitch.setTextColor(Color.WHITE);
        btnDigitSwitch.setPadding(0, 0, 0, 0);
        btnDigitSwitch.setSingleLine(true);
        setBtnSelectorBackground(btnDigitSwitch, get9PatchDrawable(bmpBtnBg2), bgTransp);
        
        for (int i = 0; i < 4; i++)
            layoutRow1.addView(btnDigits[i]);
        
        for (int i = 4; i < 7; i++)
            layoutRow2.addView(btnDigits[i]);
        
        layoutRow2.addView(btnDigitDel);
        
        for (int i = 7; i < 10; i++)
            layoutRow3.addView(btnDigits[i]);
        
        layoutRow3.addView(btnDigitSwitch);
        
        layoutDigit.addView(layoutRow1);
        layoutDigit.addView(layoutRow2);
        layoutDigit.addView(layoutRow3);
        
        
        /**  ================== 添加到键盘  ================ **/
        
        layoutDigit.setVisibility(View.GONE);
        keyboard.addView(layoutLetter);
        keyboard.addView(layoutDigit);
        
        btnLetterCap.setOnClickListener(this);
        btnOk.setOnClickListener(this);
        btnLetterDel.setOnClickListener(this);
        btnLetterSwitch.setOnClickListener(this);
        btnDigitDel.setOnClickListener(this);
        btnDigitSwitch.setOnClickListener(this);
        for (int i = 0; i < 26; i++) {
            btnLetters[i].setOnClickListener(this);
        }
        for (int i = 0; i < 10; i++) {
            btnDigits[i].setOnClickListener(this);
        }
    }

    private NinePatchDrawable get9PatchDrawable(Bitmap bmp) {
    	return new NinePatchDrawable(context.getResources(), bmp, bmp.getNinePatchChunk(), new Rect(), null);
    }
    
    private void setBtnSelectorBackground(View btn, Drawable drawableNormal, Drawable drawablePressed) {
    	StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, drawablePressed);
        sld.addState(new int[]{}, drawableNormal);
//    	sld.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.RED));
//        sld.addState(new int[]{}, new ColorDrawable(Color.YELLOW));
        btn.setBackgroundDrawable(sld);
    }
    
    /**
     * 设置安全键盘所使用的加密工具
     * @param crypto
     */
    public void setCryptTool(ICrypto crypto) {
        mCrypto = crypto;
    }
    
    public void setViewToBeShownIn(View viewToBeShownIn) {
    	this.mViewToBeShownIn = viewToBeShownIn;
    }

    public void show() {
        
    	reset();
    	
        // 收起软键盘
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etTarget.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        

        // 解决快速点击时会崩溃的问题：
        // 因为keyboard还没有从它的父VIEW中移除，所以这里判断一下，
        // 快速点击时，v是PopupWindow&PopupViewContainer类型的对象
        ViewParent v = keyboard.getParent();
        if (v != null && v instanceof ViewGroup) {
        	((ViewGroup)v).removeAllViews();
        }
        
        pop = new PopupWindow(keyboard, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        pop.setTouchable(true);
        pop.setBackgroundDrawable(new PaintDrawable());   // 设置一个空的背景（必须要）
        pop.setOutsideTouchable(true);
//        pop.setAnimationStyle(R.style.keyboard_anim);
//        pop.showAtLocation(mViewToBeShownIn, Gravity.BOTTOM, -1000, -1000);
        pop.showAsDropDown(etTarget);
        keyboard.setOnKeyListener(this);               // 监听返回键
        pop.setOnDismissListener(this);
    }

    public void bind(EditText editText) {

        etTarget.setFocusable(true);
        etTarget.setFocusableInTouchMode(true);
        etTarget.setInputType(InputType.TYPE_NULL);

        etTarget.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (etTarget.isFocused()) {
                    	show();
                    } else {
                    	etTarget.requestFocus();
                    }
                }
                return true;
            }
        });

        etTarget.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                	show();
                    InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etTarget.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });
    }
    
    /**
     *  洗牌算法，把数组随机打乱
     */
    private void shuffle(char[] arr) {
        Random rand = new Random();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            char t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
        }
    }
    
    private void initLetters() {
        letters = new char[26];
        char[] tmp = isCapLock ? orderedCapLetters : orderedLetters;
        for (int i = 0; i < 26; i++) {
            letters[i] = tmp[i];
        }
        if (!isLetterOrdered) {
            shuffle(letters);
        }
        for (int i = 0; i < 26; i++) {
            btnLetters[i].setText(String.valueOf(letters[i]));
        }
        layoutDigit.setVisibility(View.GONE);
        layoutLetter.setVisibility(View.VISIBLE);
        if (isCapLock) {
            btnLetterCap.setImageBitmap(bmpCapLocked);
        } else {
            btnLetterCap.setImageBitmap(bmpCapNormal);
        }
    }
    
    private void initDigits() {
        digits = new char[10];
        for (int i = 0; i < 10; i++) {
            digits[i] = orderedDigits[i];
        }
        if (!isDigitOrdered) {
            shuffle(digits);
        }
        for (int i = 0; i < 10; i++) {
            btnDigits[i].setText(String.valueOf(digits[i]));
        }
        layoutDigit.setVisibility(View.VISIBLE);
        layoutLetter.setVisibility(View.GONE);
    }
    
        
    /**
     *  设置最大长度
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
    
    /**
     *  设置字母键盘是否有序
     */
    public void setLetterOrdered(boolean isOrdered) {
        this.isLetterOrdered = isOrdered;
    }
    
    /**
     *  设置数字键盘是否有序
     */
    public void setDigitOrdered(boolean isOrdered) {
        this.isDigitOrdered = isOrdered;
    }
    
    /**
     *  设置是否允许输入字母
     */
    public void setLetterAllowed(boolean isAllowed) {
        this.isLetterAllowed = isAllowed;
    }
    
    /**
     *  设置是否允许输入数字
     */
    public void setDigitAllowed(boolean isAllowed) {
        this.isDigitAllowed = isAllowed;
    }
    
    /**
     *  在末尾删除一个字符
     */
    private void deleteChar() {
        if (mStrDisplay.length() > 0) {
            String tmp = mCrypto.dec(mStrEncrypt, mStrDisplay.length());
            tmp = tmp.substring(0, tmp.length() - 1);
            mStrEncrypt = mCrypto.enc(tmp);
            mStrDisplay.deleteCharAt(mStrDisplay.length() - 1);
            etPsw.setText(mStrDisplay);
            etTarget.setText(mStrDisplay);
            etTarget.setSelection(mStrDisplay.length());
        }
    }
    
    /**
     *  在末尾添加一个字符
     */
    private void appendChar(char ch) {
        if (mStrDisplay.length() < maxLength) {
            String tmp = "";
            if (mStrEncrypt != null && mStrEncrypt.length() > 0) {
                tmp = mCrypto.dec(mStrEncrypt, mStrDisplay.length());
            }
            tmp = tmp + ch;
            mStrEncrypt =  mCrypto.enc(tmp);
            etPsw.setText(mStrDisplay.append('*'));
            etTarget.setText(mStrDisplay);
            etTarget.setSelection(mStrDisplay.length());
        }
    }

    @Override
    public void onClick(View v) {
        
        if (v == btnOk) {
            if (pop != null) {
                pop.dismiss();
            }
        } else if (v == btnLetterSwitch) {
            if (isDigitAllowed) {
                initDigits();
            }
        } else if (v == btnDigitSwitch) {
            if (isLetterAllowed) {
                initLetters();
            }
        } else if (v == btnLetterCap) {
        	isCapLock = !isCapLock;
            initLetters();
        } else if (v == btnLetterDel || v == btnDigitDel) {
            deleteChar();
        } else {
            
            if (mStrDisplay.length() >= maxLength) {
                return;
            }
            
            boolean bo = false;
            char ch = '.';
            
            for (int i = 0; i < 26; i++) {
                if (v == btnLetters[i]) {
                    bo = true;
                    ch = letters[i];
                    break;
                }
            }
            
            if (!bo) {
                for (int i = 0; i < 10; i++) {
                    if (v == btnDigits[i]) {
                        bo = true;
                        ch = digits[i];
                        break;
                    }
                }
            }
            
            if (bo) {
                appendChar(ch);
            }
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            if (pop != null) {
                pop.dismiss();
            }
        }
        return false;
    }

    @Override
    public void onDismiss() {
        etTarget.setTag(TAG_KEY, mStrEncrypt);
    }
    
    
    /**
     * 默认使用的加密类（未进行任何处理，直接显示明文）
     */
    private static class DefaultCrypto implements ICrypto {

		@Override
		public String enc(String txt) {
			return txt;
		}

		@Override
		public String dec(String txt, int len) {
			return txt;
		}
    	
    }

}
