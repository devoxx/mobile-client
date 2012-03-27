package net.peterkuterna.android.apps.devoxxfrsched.ui;

import net.peterkuterna.android.apps.devoxxfrsched.ui.widget.TouchImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MapFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		int imageId = getArguments().getInt("imageId");
		Bitmap image = BitmapFactory.decodeResource(getResources(), imageId);
		TouchImageView imageView = new TouchImageView(getActivity());
		imageView.setImageBitmap(image);
		imageView.setMaxZoom(4f);
		return imageView;
	}

}
