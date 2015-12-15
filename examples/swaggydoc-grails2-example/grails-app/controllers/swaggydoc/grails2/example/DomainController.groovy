package swaggydoc.grails2.example



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import grails.rest.RestfulController

import com.github.rahulsom.swaggydoc.*
import com.wordnik.swagger.annotations.*

@Transactional(readOnly = true)
@Api(value = 'domain')
class DomainController extends RestfulController {

    static responseFormats = ['json', 'xml']

    @Override @SwaggyList
    def index() {
        super.index()
    }

    @Override @SwaggyShow
    def show() {
        super.show()
    }

    @Override @SwaggySave
    def save() {
        super.save()
    }

    @Override @SwaggyUpdate
    def update() {
        super.update()
    }

    @Override @SwaggyDelete
    def delete() {
        super.delete()
    }

    @Override @SwaggyPatch
    Object patch() {
        return super.patch()
    }
}
