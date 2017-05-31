package com.penn.ppj;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomSheetDialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.ppj.databinding.FragmentCommentInputBottomSheetDialogBinding;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class CommentInputBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private FragmentCommentInputBottomSheetDialogBinding binding;
    public CommentViewModel commentViewModel;

    private CommentInputBottomSheetDialogFragmentListener commentInputBottomSheetDialogFragmentListener;

    public interface CommentInputBottomSheetDialogFragmentListener {
        public void setCommentViewModel(CommentViewModel commentViewModel);

        public void sendComment();
    }

    public static class CommentViewModel {
        public String targetUserId = "";
        public String targetNickname = "";
        public String content;
        public boolean bePrivate;

        public String getHint() {
            String targetStr = "";
            if (!TextUtils.isEmpty(targetNickname)) {
                targetStr = "@" + targetNickname + " ";
            }

            return targetStr;
        }

        public boolean validate() {
            return !TextUtils.isEmpty(content);
        }

        public void reset() {
            targetUserId = "";
            targetNickname = "";
            content = "";
            bePrivate = false;
        }
    }

    public static CommentInputBottomSheetDialogFragment newInstance(CommentViewModel commentViewModel) {
        CommentInputBottomSheetDialogFragment fragment = new CommentInputBottomSheetDialogFragment();
        Bundle bundle = new Bundle();
        String tmpStr = new Gson().toJson(commentViewModel);
        bundle.putString("commentViewModel", tmpStr);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_comment_input_bottom_sheet_dialog, container, false);

        setup();

        return binding.getRoot();
    }

    public void setCommentInputBottomSheetDialogFragmentListener(CommentInputBottomSheetDialogFragmentListener commentInputBottomSheetDialogFragmentListener) {
        this.commentInputBottomSheetDialogFragmentListener = commentInputBottomSheetDialogFragmentListener;
    }

    private void setup() {
        //set to adjust screen height automatically, when soft keyboard appears on screen
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        String commentViewModelStr = getArguments().getString("commentViewModel");
        commentViewModel = new Gson().fromJson(commentViewModelStr, CommentViewModel.class);
        binding.setData(commentViewModel);

        //这里要用delay, 怀疑binding.setData(commentViewModel);是个异步操作, 需要等这个异步操作完成后定位光标才有意义
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                binding.contentTextInputEditText.setSelection(binding.contentTextInputEditText.getText().length());
            }
        }, 100);

        //content输入监控
        RxTextView.textChanges(binding.contentTextInputEditText)
                .skip(1)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<CharSequence>() {
                            @Override
                            public void accept(@NonNull CharSequence charSequence) throws Exception {
                                commentViewModel.content = charSequence.toString();
                                binding.setData(commentViewModel);
                                commentInputBottomSheetDialogFragmentListener.setCommentViewModel(commentViewModel);
                            }
                        }
                );

        //bePrivate监控
        RxCompoundButton.checkedChanges(binding.privateCheckBox)
                .skip(1)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Boolean>() {
                            @Override
                            public void accept(@NonNull Boolean aBoolean) throws Exception {
                                commentViewModel.bePrivate = aBoolean;
                                binding.setData(commentViewModel);
                                commentInputBottomSheetDialogFragmentListener.setCommentViewModel(commentViewModel);
                            }
                        }
                );

        //send comment按钮监控
        Observable<Object> commentButtonObservable = RxView.clicks(binding.sendButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        commentButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                commentInputBottomSheetDialogFragmentListener.sendComment();
                                dismiss();
                            }
                        }
                );

        binding.contentTextInputEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    if (TextUtils.isEmpty(commentViewModel.content)) {
                        //do nothing
                        return true;
                    }

                    commentInputBottomSheetDialogFragmentListener.sendComment();
                    dismiss();
                    return true;
                }
                return false;
            }
        });
    }
}
