package swaggydoc.grails3.example

import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class DomainController {

    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Domain.list(params), model:[domainCount: Domain.count()]
    }

    def show(Domain domain) {
        respond domain
    }

    def create() {
        respond new Domain(params)
    }

    @Transactional
    def save(Domain domain) {
        if (domain == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (domain.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond domain.errors, view:'create'
            return
        }

        domain.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'domain.label', default: 'Domain'), domain.id])
                redirect domain
            }
            '*' { respond domain, [status: CREATED] }
        }
    }

    def edit(Domain domain) {
        respond domain
    }

    @Transactional
    def update(Domain domain) {
        if (domain == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        if (domain.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond domain.errors, view:'edit'
            return
        }

        domain.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'domain.label', default: 'Domain'), domain.id])
                redirect domain
            }
            '*'{ respond domain, [status: OK] }
        }
    }

    @Transactional
    def delete(Domain domain) {

        if (domain == null) {
            transactionStatus.setRollbackOnly()
            notFound()
            return
        }

        domain.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'domain.label', default: 'Domain'), domain.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'domain.label', default: 'Domain'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
}
