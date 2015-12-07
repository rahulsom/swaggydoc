package com.github.rahulsom.swaggydoc

import spock.lang.Specification

/**
 * Created by dpisoni on 2/20/15.
 */
class MarshallersPluginSupportSpec extends Specification {

    void "domains list required properties correctly" () {
        given: "A controller"
        def controller = new ApiController()

        when: "Resources are listed"
        controller.params.id = 'marshalled'
        controller.show()

        then: "An expected json is returned"
        def json = controller.response.json
        json.models.size() == 1
        def domainModel = json.models['Marshalled']

        domainModel['id'] == 'Marshalled'
        def domainProps = domainModel['properties']
        domainModel['required'] as Set == ['firstName', 'lastName', 'id'] as Set
        domainProps.size() == 6

        // Identity
        domainProps.id
        domainProps.id.format == 'int64'
        domainProps.id.type == 'integer'

        // "Real" fields
        domainProps.firstName
        domainProps.firstName.type == 'string'
        domainProps.lastName
        domainProps.lastName.type == 'string'
        domainProps.prefix
        domainProps.prefix.type == 'string'
        domainProps.suffix
        domainProps.suffix.type == 'string'

        // virtual fields
        domainProps.fullName
        domainProps.fullName.type == 'string'

        // ignored/suppressed fields
        domainProps.getAt('sortOrder') == null
        domainProps.getAt('version') == null

    }
}
