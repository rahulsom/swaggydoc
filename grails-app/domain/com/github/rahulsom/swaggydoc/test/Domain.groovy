package com.github.rahulsom.swaggydoc.test

class Domain {

	String name
	String description

	List subdomains

	static hasMany = [subdomains: Subdomain]
	static constraints = {
		description nullable: true
	}
}
