package com.github.rahulsom.swaggydoc.test

import com.wordnik.swagger.annotations.ApiModel
import com.wordnik.swagger.annotations.ApiModelProperty

@ApiModel(value = 'Category')
class Category {
    @ApiModelProperty(value = 'Name of category', required = true)
    String name

//@ApiModelProperty(value = 'Parent categories', required = false)
//Set parents = []

    static hasMany = [parents: Category]

    static constraints = {
        name nullable: false, blank: false
    }

    static mapping = {
        table 'categories'
        id generator: 'identity'
        sort 'name'
        cache true
        autoImport false

/*parents(
  joinTable : [name : 'categories_hierarchy', key : 'node_id', column : 'parent_id']
)*/
    }
}

