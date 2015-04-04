package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.ApiResponse

/**
 * Created by rahul on 4/3/15.
 */
class ResponseMessage {
    int code
    String message

    ResponseMessage(int code, String message) {
        this.code = code
        this.message = message
    }

    ResponseMessage(ApiResponse response) {
        this(response.code(), response.message())
    }
}
