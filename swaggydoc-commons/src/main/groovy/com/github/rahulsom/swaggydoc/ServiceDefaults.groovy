package com.github.rahulsom.swaggydoc

import org.springframework.http.HttpStatus

import java.util.function.Function

/**
 * Created by rahul on 12/6/15.
 */
class ServiceDefaults {
    /*
     * These don't get documented in the listing of models.
     */
    protected static final List knownTypes = [
            int, Integer, long, Long, float, Float, double, Double, BigInteger, BigDecimal,
            String, boolean, Boolean, Date, byte, Byte, void
    ]
    public static final Map<String, Function<String, DefaultAction>> DefaultActionComponents = [
            index : { String domainName ->
                new DefaultAction(SwaggyList, domainName, [
                        new Parameter('offset', 'Records to skip. Empty means 0.', 'query', 'int'),
                        new Parameter('max', 'Max records to return. Empty means 10.', 'query', 'int'),
                        new Parameter('sort', 'Field to sort by. Empty means id if q is empty. If q is provided, empty ' +
                                'means relevance.', 'query', 'string'),
                        new Parameter('order', 'Order to sort by. Empty means asc if q is empty. If q is provided, empty ' +
                                'means desc.', 'query', 'string').with {
                            _enum = ['asc', 'desc']
                            it
                        },
                ], [], true)
            },
            show  : { String domainName ->
                new DefaultAction(SwaggyShow, domainName, [new Parameter('id', 'Identifier to look for', 'path', 'string', true)],
                        [
                                new ResponseMessage(HttpStatus.BAD_REQUEST, 'Bad Request'),
                                new ResponseMessage(HttpStatus.NOT_FOUND, "Could not find ${domainName} with that Id"),
                        ]
                )
            },
            save  : { String domainName ->
                new DefaultAction(SwaggySave, domainName, [new Parameter('body', "Description of ${domainName}", 'body', domainName, true)],
                        [
                                new ResponseMessage(HttpStatus.CREATED, "New ${domainName} created"),
                                new ResponseMessage(HttpStatus.UNPROCESSABLE_ENTITY, 'Malformed Entity received'),
                        ]
                )
            },
            update: { String domainName ->
                new DefaultAction(SwaggyUpdate, domainName,
                        [
                                new Parameter('id', "Id to update", 'path', 'string', true),
                                new Parameter('body', "Description of ${domainName}", 'body', domainName, true),
                        ],
                        [
                                new ResponseMessage(HttpStatus.BAD_REQUEST, 'Bad Request'),
                                new ResponseMessage(HttpStatus.NOT_FOUND, "Could not find ${domainName} with that Id"),
                                new ResponseMessage(HttpStatus.UNPROCESSABLE_ENTITY, 'Malformed Entity received'),
                        ]
                )
            },
            patch : { String domainName ->
                new DefaultAction(SwaggyPatch, domainName,
                        [
                                new Parameter('id', "Id to patch", 'path', 'string', true),
                                new Parameter('body', "Description of ${domainName}", 'body', domainName, true),
                        ],
                        [
                                new ResponseMessage(HttpStatus.BAD_REQUEST, 'Bad Request'),
                                new ResponseMessage(HttpStatus.NOT_FOUND, "Could not find ${domainName} with that Id"),
                                new ResponseMessage(HttpStatus.UNPROCESSABLE_ENTITY, 'Malformed Entity received'),
                        ]
                )
            },
            delete: { String domainName ->
                new DefaultAction(SwaggyDelete, 'void', [new Parameter('id', "Id to delete", 'path', 'string', true)],
                        [
                                new ResponseMessage(HttpStatus.NO_CONTENT, 'Delete successful'),
                                new ResponseMessage(HttpStatus.BAD_REQUEST, 'Bad Request'),
                                new ResponseMessage(HttpStatus.NOT_FOUND, "Could not find ${domainName} with that Id"),
                        ]
                )
            }
    ].collectEntries {k, v -> [k, v as Function<String, DefaultAction>]}

    static List<String> removeBoringMethods(List<String> methods, List<String> boringMethods) {
        boringMethods.
                each { method ->
                    if (methods.size() > 1 && methods.contains(method)) {
                        methods.remove(method)
                    }
                }
        methods
    }

    static String slugToDomain(String slug) {
        slug.with { it.replaceFirst(it[0], it[0].toUpperCase()) }
    }
}
