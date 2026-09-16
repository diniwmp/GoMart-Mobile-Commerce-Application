package lk.zenova.gomart.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.zenova.gomart.databinding.FragmentNoInternetBinding;

public class NoInternetFragment extends Fragment {

    private FragmentNoInternetBinding binding;
    private OnRetryListener retryListener;

    public interface OnRetryListener {
        void onRetry();
    }

    public void setOnRetryListener(OnRetryListener listener) {
        this.retryListener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNoInternetBinding.inflate(
                inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnRetry.setOnClickListener(v -> {

            binding.retryProgress.setVisibility(View.VISIBLE);
            binding.btnRetry.setEnabled(false);

            new android.os.Handler(
                    android.os.Looper.getMainLooper())
                    .postDelayed(() -> {
                        if (binding == null) return;
                        binding.retryProgress.setVisibility(View.GONE);
                        binding.btnRetry.setEnabled(true);

                        if (isInternetAvailable()) {
                            if (retryListener != null) retryListener.onRetry();
                        }
                    }, 1500);
        });
    }

    private boolean isInternetAvailable() {
        android.net.ConnectivityManager cm =
                (android.net.ConnectivityManager)
                        requireContext().getSystemService(
                                android.content.Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        android.net.NetworkCapabilities caps =
                cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null
                && caps.hasCapability(
                android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}