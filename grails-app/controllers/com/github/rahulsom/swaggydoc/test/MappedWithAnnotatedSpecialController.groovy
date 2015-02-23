package com.github.rahulsom.swaggydoc.test

import com.wordnik.swagger.annotations.Api
import com.wordnik.swagger.annotations.ApiImplicitParam
import com.wordnik.swagger.annotations.ApiImplicitParams
import com.wordnik.swagger.annotations.ApiOperation
import grails.rest.RestfulController
import grails.transaction.Transactional

@Transactional(readOnly = true)
@Api(value = 'mapped with annotated "special" action')
class MappedWithAnnotatedSpecialController extends RestfulController {

    // Unknown response format will be ignored by SwaggyDataService, unless it has a '/'
    static responseFormats = ['json', 'xml', 'gleeborp', 'application/x-gleeborp']

    MappedWithAnnotatedSpecialController() {
        super(Domain)
    }

    @ApiOperation(value = "Muy Especial", response = Domain)
    @ApiImplicitParams([
        @ApiImplicitParam(name = 'dingbat', paramType = 'query', required = true, dataType = 'int')
    ])
    def especial() {
        return get(params.get('dingbat'))
    }
}
