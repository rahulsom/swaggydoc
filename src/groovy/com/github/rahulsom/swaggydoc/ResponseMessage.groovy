package com.github.rahulsom.swaggydoc

import com.wordnik.swagger.annotations.ApiResponse
import org.springframework.http.HttpStatus

/**
 * A Response message based on http response codes
 *
 * @author Rahul
 */
class ResponseMessage {
    int code
    String message

    ResponseMessage(int code, String message) {
        this.code = code
        this.message = message
    }

    ResponseMessage(HttpStatus code, String message) {
        this(code.value(), message)
    }

    ResponseMessage(ApiResponse response) {
        this(response.code(), response.message())
    }
}
