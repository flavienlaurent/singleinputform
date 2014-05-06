package com.flavienlaurent.minimalform.app;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Property;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.IconTextView;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class MainActivity extends Activity {

    private static final Step[] sSteps = {
            new Step(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, R.string.email, R.string.email_error, R.string.email_details, new StepChecker() {
                @Override
                public boolean check(String input) {
                    return android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();
                }
            }),
            new Step(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, R.string.password, R.string.password_error, R.string.password_details, new StepChecker() {
                @Override
                public boolean check(String input) {
                    return input.length() >= 6;
                }
            }),
            new Step(InputType.TYPE_CLASS_NUMBER, R.string.year_of_birth, R.string.year_of_birth_error, R.string.year_of_birth_details, new StepChecker() {
                @Override
                public boolean check(String input) {
                    return TextUtils.isDigitsOnly(input) && input.length() == 4;
                }
            }),
            new Step(InputType.TYPE_CLASS_TEXT, R.string.city, R.string.city_error, R.string.city_details)
    };

    private int mStepIndex = 0;
    private boolean mErrored;

    private TextSwitcher mTitleSwitcher;
    private TextSwitcher mErrorSwitcher;
    private TextSwitcher mDetailsSwitcher;
    private EditText mInput;
    private IconTextView mNextButton;
    private ProgressBar mProgressbar;
    private TextView mStepText;
    private View mCompletedView;
    private View mRetryButton;

    private View.OnClickListener mOnNextButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            nextStep();
        }
    };

    private View.OnClickListener mOnRetryButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            hideFinalView();
            mStepIndex = 0;
            updateStep();
        }
    };

    private TextView.OnEditorActionListener mOnInputEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                nextStep();
                return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();

        setupTitle();
        setupError();
        setupDetails();

        mInput.setOnEditorActionListener(mOnInputEditorActionListener);
        mNextButton.setOnClickListener(mOnNextButtonClickListener);
        mRetryButton.setOnClickListener(mOnRetryButtonClickListener);
        mErrorSwitcher.setText("");

        if(savedInstanceState == null) {
            mStepIndex = 0;
        } else {
            mStepIndex = savedInstanceState.getInt("step_index", 0);
        }
        updateStep();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("step_index", mStepIndex);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mStepIndex = savedInstanceState.getInt("step_index", 0);
        updateStep();
    }

    private void nextStep() {
        Step step = sSteps[mStepIndex];
        boolean checkStep = checkStep();
        if(!checkStep) {
            if(!mErrored) {
                mErrored = true;
                mErrorSwitcher.setText(getString(step.mErrorResId));
            }
        } else{
            mErrored = false;
        }
        if(mErrored) {
            return;
        }

        mStepIndex++;
        updateStep();
        mInput.setText("");
    }

    private void updateStep() {
        if(mStepIndex >= maxStep()) {
            InputMethodManager imm = (InputMethodManager)getSystemService( Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
            showFinalView();
            return;
        }
        updateViews();
    }

    private void hideFinalView() {
        mCompletedView.animate().alpha(0f).withEndAction(new Runnable() {
            @Override
            public void run() {
                mCompletedView.setVisibility(View.GONE);
            }
        });
    }

    private void showFinalView() {
        mCompletedView.setAlpha(0.0f);
        mCompletedView.setVisibility(View.VISIBLE);
        mCompletedView.animate().alpha(1f);
    }

    private void updateViews() {
        if(mStepIndex +1 >= maxStep()) {
            mNextButton.setText("{fa-check}");
            mInput.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        } else {
            mNextButton.setText("{fa-arrow-right}");
            mInput.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        }
        mErrorSwitcher.setText("");
        Step step = sSteps[mStepIndex];
        mInput.setInputType(step.mInputType);
        mDetailsSwitcher.setText(getString(step.mDetailsResId));
        mTitleSwitcher.setText(getString(step.mTitleResId));
        mStepText.setText((mStepIndex +1) + "/" + maxStep());
        updateProgressbar();
    }

    private static final Property<ProgressBar, Integer> PB_PROGRESS_PROPERTY =
            new Property<ProgressBar, Integer>(Integer.class, "PB_PROGRESS_PROPERTY") {

                @Override
                public void set(ProgressBar pb, Integer value) {
                    pb.setProgress(value);
                }

                @Override
                public Integer get(ProgressBar pb) {
                    return pb.getProgress();
                }
            };

    private void updateProgressbar() {
        mProgressbar.setMax(maxStep() * 100);
        ObjectAnimator.ofInt(mProgressbar, PB_PROGRESS_PROPERTY, mStepIndex *100).start();
    }

    private boolean checkStep() {
        String inputText = mInput.getText().toString();
        if(TextUtils.isEmpty(inputText)) {
            return false;
        }
        return sSteps[mStepIndex].mChecker.check(inputText);
    }

    private int maxStep() {
        return sSteps.length;
    }

    private void setupError() {
        mErrorSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left));
        mErrorSwitcher.setOutAnimation( AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));

        mErrorSwitcher.setFactory(new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                return getLayoutInflater().inflate(R.layout.view_error, null);
            }});

        mErrorSwitcher.setText("");
    }

    private void setupTitle() {
        mTitleSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_to_bottom));
        mTitleSwitcher.setOutAnimation( AnimationUtils.loadAnimation(this, R.anim.slide_out_to_top));

        mTitleSwitcher.setFactory(new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                return getLayoutInflater().inflate(R.layout.view_title, null);
            }
        });

        mTitleSwitcher.setText("");
    }

    private void setupDetails() {
        mDetailsSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.alpha_in));
        mDetailsSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.alpha_out));

        mDetailsSwitcher.setFactory(new ViewSwitcher.ViewFactory() {

            @Override
            public View makeView() {
                return getLayoutInflater().inflate(R.layout.view_details, null);
            }});

        mDetailsSwitcher.setText("");
    }

    private void findViews() {
        mTitleSwitcher = (TextSwitcher) findViewById(R.id.title_switcher);
        mErrorSwitcher = (TextSwitcher) findViewById(R.id.error_switcher);
        mDetailsSwitcher = (TextSwitcher) findViewById(R.id.details_switcher);
        mInput = (EditText) findViewById(R.id.input);
        mNextButton = (IconTextView) findViewById(R.id.next_button);
        mProgressbar = (ProgressBar) findViewById(R.id.progressbar);
        mStepText = (TextView) findViewById(R.id.step_text);
        mCompletedView = findViewById(R.id.completed);
        mRetryButton = findViewById(R.id.retry);
    }

    private static final class Step {
        private int mInputType;
        private int mTitleResId;
        private int mErrorResId;
        private int mDetailsResId;
        private final StepChecker mChecker;

        private Step(int inputType, int titleResId, int errorResId, int detailsResId, StepChecker checker) {
            mInputType = inputType;
            mTitleResId = titleResId;
            mErrorResId = errorResId;
            mDetailsResId = detailsResId;
            mChecker = checker;
        }

        private Step(int type, int titleResId, int errorResId, int detailsResId) {
            this(type, titleResId, errorResId, detailsResId, new StepChecker() {
                @Override
                public boolean check(String input) {
                    return true;
                }
            });
        }
    }

    private interface StepChecker {
        boolean check(String input);
    }
}
