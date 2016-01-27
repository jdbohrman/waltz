/*
 *  This file is part of Waltz.
 *
 *     Waltz is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Waltz is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Waltz.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.khartec.waltz.web.endpoints.api;

import com.khartec.waltz.common.ListUtilities;
import com.khartec.waltz.model.EntityKind;
import com.khartec.waltz.model.ImmutableEntityReference;
import com.khartec.waltz.model.Severity;
import com.khartec.waltz.model.application.AppRegistrationRequest;
import com.khartec.waltz.model.application.AppRegistrationResponse;
import com.khartec.waltz.model.application.Application;
import com.khartec.waltz.model.application.AssetCodeRelationshipKind;
import com.khartec.waltz.model.changelog.ImmutableChangeLog;
import com.khartec.waltz.model.tally.Tally;
import com.khartec.waltz.service.application.ApplicationService;
import com.khartec.waltz.service.changelog.ChangeLogService;
import com.khartec.waltz.web.DatumRoute;
import com.khartec.waltz.web.ListRoute;
import com.khartec.waltz.web.action.AppChangeAction;
import com.khartec.waltz.web.endpoints.Endpoint;
import com.khartec.waltz.web.WebUtilities;
import com.khartec.waltz.web.endpoints.EndpointUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spark.Route;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.khartec.waltz.common.Checks.checkNotNull;
import static com.khartec.waltz.web.WebUtilities.getId;
import static com.khartec.waltz.web.WebUtilities.mkPath;
import static com.khartec.waltz.web.WebUtilities.readBody;
import static com.khartec.waltz.web.endpoints.EndpointUtilities.*;
import static java.lang.Long.parseLong;


@Service
public class ApplicationEndpoint implements Endpoint {


    private static final Logger LOG = LoggerFactory.getLogger(ApplicationEndpoint.class);

    private static final String BASE_URL = mkPath("api", "app");

    private final ApplicationService appService;
    private final ChangeLogService changeLogService;


    @Autowired
    public ApplicationEndpoint(ApplicationService appService,
                               ChangeLogService changeLogService) {
        checkNotNull(appService, "appService must not be null");
        checkNotNull(changeLogService, "changeLogService must not be null");

        this.appService = appService;
        this.changeLogService = changeLogService;
    }


    @Override
    public void register() {
        registerQueries();
        registerAppRegistration();
        registerAppUpdate();
    }


    private void registerAppUpdate() {
        Route handleAppUpdateRequest = (req, res) -> {
            res.type(WebUtilities.TYPE_JSON);
            AppChangeAction appChange = readBody(req, AppChangeAction.class);
            LOG.info("Updating application: " + appChange);

            Long appId = appChange.app().id().get();

            appChange.changes().forEach(c -> changeLogService.write(
                    ImmutableChangeLog.builder()
                        .message(c.toDescription())
                        .severity(Severity.INFORMATION)
                        .userId(WebUtilities.getUser(req).userName())
                        .parentReference(ImmutableEntityReference.builder()
                                .kind(EntityKind.APPLICATION)
                                .id(appId)
                                .build())
                        .build()));


            appService.update(appChange.app());
            appService.updateTags(appId, appChange.tags());
            appService.updateAliases(appId, appChange.aliases());

            return true;
        };

        post(mkPath(BASE_URL, ":id"), handleAppUpdateRequest);

    }


    private void registerAppRegistration() {
        Route handleRegistrationRequest = (req, res) -> {
            res.type(WebUtilities.TYPE_JSON);
            AppRegistrationRequest registrationRequest = readBody(req, AppRegistrationRequest.class);

            LOG.info("Registering new application:" + registrationRequest);

            AppRegistrationResponse registrationResponse = appService
                    .registerApp(registrationRequest);


            changeLogService.write(ImmutableChangeLog.builder()
                    .message("Registered new application: " + registrationRequest.name())
                    .severity(Severity.INFORMATION)
                    .userId(WebUtilities.getUser(req).userName())
                    .parentReference(ImmutableEntityReference.builder()
                            .kind(EntityKind.APPLICATION)
                            .id(registrationResponse.id().get())
                            .build())
                    .build());

            return registrationResponse;
        };

        post(BASE_URL, handleRegistrationRequest);
    }


    private void registerQueries() {
        ListRoute<Application> search = (request, response) -> appService.search(request.params("query"));

        ListRoute<Tally> tallyByOrgUnit = (request, response) -> appService.countByOrganisationalUnit();

        ListRoute<Application> findByOrgUnit = (req, res) -> {
            String ouId = req.params("ouId");
            return appService
                    .findByOrganisationalUnitId(parseLong(ouId));
        };

        ListRoute<Application> findByOrgUnitTree = (req, res) -> {
            String ouId = req.params("ouId");
            return appService
                    .findByOrganisationalUnitTree(parseLong(ouId));
        };

        DatumRoute<Map<AssetCodeRelationshipKind, List<Application>>> findRelated
                = (req, res) -> appService.findRelated(getId(req));

        ListRoute<Application> findByIds = (req, res) -> {
            List<Long> ids = (List<Long>) readBody(req, List.class);
            if (ListUtilities.isEmpty(ids)) {
                return Collections.emptyList();
            }
            return appService
                    .findByIds(ids);
        };

        DatumRoute<Application> getById = (req, res) -> {
            String id = req.params("id");
            return appService
                    .getById(parseLong(id));
        };

        ListRoute<String> findAllTags = (request, response) ->
                appService.findAllTags();

        ListRoute<Application> findByTag = (request, response) ->
                appService.findByTag(request.body());

        getForList(mkPath(BASE_URL, "search", ":query"), search);
        getForList(mkPath(BASE_URL, "org-unit", ":ouId"), findByOrgUnit);
        getForList(mkPath(BASE_URL, "org-unit-tree", ":ouId"), findByOrgUnitTree);
        getForList(mkPath(BASE_URL, "count-by", "org-unit"), tallyByOrgUnit);
        getForList(mkPath(BASE_URL, "tags"), findAllTags);

        getForDatum(mkPath(BASE_URL, "id", ":id"), getById);
        getForDatum(mkPath(BASE_URL, "id", ":id", "related"), findRelated);

        postForList(mkPath(BASE_URL, "tags"), findByTag);  // POST as may not be good for qparam
        postForList(mkPath(BASE_URL, "by-ids"), findByIds);
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApplicationEndpoint{");
        sb.append('}');
        return sb.toString();
    }
}