package swaggydoc.grails2.example



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional
import grails.rest.RestfulController

@Transactional(readOnly = true)
class DomainController extends RestfulController {

    static responseFormats = ['json', 'xml']

    @Override
    def index() {
        super.index()
    }

    @Override
    def show() {
        super.show()
    }

    @Override
    def save() {
        super.save()
    }

    @Override
    def update() {
        super.update()
    }

    @Override
    def delete() {
        super.delete()
    }

    @Override
    Object patch() {
        return super.patch()
    }
}
