
# Settings

## Security

* ```web.authentication```
  * ```waltz``` - indicates that waltz is handling authentication, the login panel will be displayed
  * ```sso``` - authentication is done elsewhere, do not show the login panel

* ```server.authentication.filter```
  * ```<classname>``` - the name of the class which injects the user object into incoming requests.
    By default this is ```com.khartec.waltz.web.endpoints.auth.JWTAuthenticationFilter```
  * ```com.khartec.waltz.web.endpoints.auth.HeaderBasedAuthenticationFilter```
  * ```com.khartec.waltz.web.endpoints.auth.JWTAuthenticationFilter```


## Development (```web.devext``` )

* ```web.devext.enabled``` - master switch, if enabled other settings in this group will be activated
* ```web.devext.http.header.*``` - values in this group will be added to the header of
    all $http calls (primarily api calls)
