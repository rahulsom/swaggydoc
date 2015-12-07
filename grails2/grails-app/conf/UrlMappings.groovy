class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }

        "/albums"(resources: 'album') {
            "/photos"(resources: "photo")
        }

        "/"(view: "/index")
        "500"(view: '/error')
    }
}
