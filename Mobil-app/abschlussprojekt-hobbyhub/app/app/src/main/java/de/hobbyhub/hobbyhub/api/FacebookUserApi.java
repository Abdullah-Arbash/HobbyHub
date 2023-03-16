package de.hobbyhub.hobbyhub.api;

import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;

public class FacebookUserApi {

    public void asyncGetUserByAccessTokenWithCallback(AccessToken accessToken, GraphRequest.Callback callback) {
        Bundle params = new Bundle();
        params.putString("fields", "picture,name,id,email,permissions");
        GraphRequest profileRequest = new GraphRequest(
                accessToken,
                "/me",
                params,
                HttpMethod.GET,
                callback
        );
        profileRequest.executeAsync();
    }
}
