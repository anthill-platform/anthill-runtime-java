package org.anthillplatform.runtime.services;

import org.anthillplatform.runtime.AnthillRuntime;
import org.anthillplatform.runtime.requests.JsonRequest;
import org.anthillplatform.runtime.requests.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Social service for Anthill platform
 *
 * See https://github.com/anthill-platform/anthill-social
 */
public class SocialService extends Service
{
    public static final String ID = "social";
    public static final String API_VERSION = "0.2";

    /**
     * Please note that you should not create an instance of the service yourself,
     * and use AnthillRuntime.Get(SocialService.ID, SocialService.class) to get existing one instead
     */
    public SocialService(AnthillRuntime runtime, String location)
    {
        super(runtime, location, ID, API_VERSION);
    }

    public static SocialService Get()
    {
        return AnthillRuntime.Get(ID, SocialService.class);
    }

    public interface GroupGetCallback
    {
        void complete(SocialService service, Request request, Request.Result result, Group group);
    }

    public interface GroupGetProfileCallback
    {
        void complete(SocialService service, Request request, Request.Result result,
                      JSONObject profile, boolean participant);
    }

    public interface GroupGetParticipationCallback
    {
        void complete(SocialService service, Request request, Request.Result result,
                      Group.Participant participant, boolean owner);
    }

    public interface GroupUpdateCallback
    {
        void complete(SocialService service, Request request, Request.Result result, JSONObject updatedProfile);
    }

    public interface GroupBatchUpdateCallback
    {
        void complete(SocialService service, Request request, Request.Result result,
                      Map<String, JSONObject> updatedProfiles);
    }

    public interface GroupUpdateParticipantCallback
    {
        void complete(SocialService service, Request request, Request.Result result, JSONObject updatedProfile);
    }

    public interface GroupUpdateParticipantPermissionsCallback
    {
        void complete(SocialService service, Request request, Request.Result result);
    }

    public interface GroupJoinCallback
    {
        void complete(SocialService service, Request request, Request.Result result);
    }

    public interface GroupUpdateSummaryCallback
    {
        void complete(SocialService service, Request request, Request.Result result);
    }

    public interface GroupJoinRequestCallback
    {
        void complete(SocialService service, Request request, Request.Result result, String key);
    }

    public interface GroupInviteCallback
    {
        void complete(SocialService service, Request request, Request.Result result, String key);
    }

    public interface GroupJoinApproveCallback
    {
        void complete(SocialService service, Request request, Request.Result result);
    }

    public interface GroupJoinRejectCallback
    {
        void complete(SocialService service, Request request, Request.Result result);
    }

    public interface GroupLeaveCallback
    {
        void complete(SocialService service, Request request, Request.Result result);
    }

    public interface GroupKickCallback
    {
        void complete(SocialService service, Request request, Request.Result result);
    }

    public interface GroupCreateCallback
    {
        void complete(SocialService service, Request request, Request.Result result, String newGroupId);
    }

    public interface GroupSearchCallback
    {
        void complete(SocialService service, Request request, Request.Result result, List<Group> groups);
    }

    public static class Group
    {
        private String id;
        private String name;
        private JSONObject profile;
        private JoinMethod joinMethod;
        private int freeMembers;
        private String owner;
        private HashMap<String, Participant> participants;
        private Participant me;
        private MessageService.MessageDestination messageDestination;

        public enum JoinMethod
        {
            free, approve, invite
        }

        public static class Participant
        {
            private JSONObject profile;
            private int role;
            private Set<String> permissions;

            private Participant(JSONObject data)
            {
                this.permissions = new HashSet<String>();
                this.role = data.optInt("role", 0);
                this.profile = data.optJSONObject("profile");

                JSONArray p = data.optJSONArray("permissions");
                if (p != null)
                {
                    for (int i = 0, l = p.length(); i < l; i++)
                    {
                        String permission = p.optString(i);
                        if (permission == null || permission.isEmpty())
                            continue;
                        this.permissions.add(permission);
                    }
                }
            }

            public JSONObject getProfile()
            {
                return profile;
            }

            public int getRole()
            {
                return role;
            }

            public Set<String> getPermissions()
            {
                return permissions;
            }

            public boolean hasPermission(String permission)
            {
                return permissions.contains(permission);
            }
        }

        public Group(JSONObject data)
        {
            this.me = null;
            this.messageDestination = null;

            JSONObject group = data.optJSONObject("group");

            if (group != null)
            {
                this.id = group.optString("group_id");
                this.name = group.optString("name");
                this.profile = group.optJSONObject("profile");
                this.joinMethod = JoinMethod.valueOf(group.optString("join_method", JoinMethod.free.toString()));
                this.freeMembers = group.optInt("free_members", 0);
                this.owner = group.optString("owner", null);
            }

            JSONObject participants = data.optJSONObject("participants");

            if (participants != null)
            {
                this.participants = new HashMap<String, Participant>();

                for (Object account : participants.keySet())
                {
                    JSONObject p = participants.optJSONObject(account.toString());

                    if (p != null)
                    {
                        this.participants.put(account.toString(), new Participant(p));
                    }
                }
            }

            if (data.has("me"))
            {
                JSONObject me = data.optJSONObject("me");

                if (me != null)
                {
                    this.me = new Participant(me);
                }
            }

            if (data.has("message"))
            {
                JSONObject messageDestination = data.optJSONObject("message");
                this.messageDestination = new MessageService.MessageDestination(messageDestination);
            }
        }

        public String getOwner()
        {
            return owner;
        }

        public JSONObject getProfile()
        {
            return profile;
        }

        public int getFreeMembers()
        {
            return freeMembers;
        }

        public boolean isFreeMembersLeft()
        {
            return freeMembers > 0;
        }

        public JoinMethod getJoinMethod()
        {
            return joinMethod;
        }

        public HashMap<String, Participant> getParticipants()
        {
            return participants;
        }

        public Participant getMe()
        {
            return me;
        }

        public String getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }
    }

    public void getGroup(
        LoginService.AccessToken accessToken,
        String groupId,
        final GroupGetCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/group/" + groupId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    Group group = new Group(((JsonRequest) request).getObject());
                    callback.complete(SocialService.this, request, result, group);
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getGroupProfile(
        LoginService.AccessToken accessToken,
        String groupId,
        final GroupGetProfileCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/group/" + groupId + "/profile",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    boolean participant = response.optBoolean("participant");
                    JSONObject group = response.optJSONObject("group");

                    if (group == null)
                    {
                        callback.complete(SocialService.this, request, Request.Result.dataCorrupted, null, false);
                        return;
                    }

                    JSONObject profile = group.optJSONObject("profile");
                    callback.complete(SocialService.this, request, result, profile, participant);
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null, false);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void getMyGroupParticipant(
        LoginService.AccessToken accessToken,
        String groupId,
        final GroupGetParticipationCallback callback)
    {
        getGroupParticipant(accessToken, groupId, "me", callback);
    }

    public void getGroupParticipant(
        LoginService.AccessToken accessToken, String groupId,
        String accountId,
        final GroupGetParticipationCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/group/" + groupId + "/participation/" + accountId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject participation = response.optJSONObject("participation");
                    boolean owner = response.optBoolean("owner", false);

                    if (participation != null)
                    {
                        callback.complete(SocialService.this, request, result,
                            new Group.Participant(participation), owner);
                    }
                    else
                    {
                        callback.complete(SocialService.this, request, Request.Result.dataCorrupted, null, false);
                    }
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null, false);
                }
            }
        });

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void updateGroupProfile(
        LoginService.AccessToken accessToken, String groupId,
        JSONObject groupProfile,
        final GroupUpdateCallback profileCallback)
    {
        updateGroupProfile(accessToken, groupId, groupProfile, null, true, profileCallback);
    }

    public void updateGroupProfile(
        LoginService.AccessToken accessToken, String groupId,
        JSONObject groupProfile,
        JSONObject notify,
        boolean merge,
        final GroupUpdateCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/group/" + groupId + "/profile",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject group = response.optJSONObject("group");
                    if (group != null)
                    {
                        JSONObject groupProfile = group.optJSONObject("profile");

                        if (groupProfile != null)
                        {
                            callback.complete(SocialService.this, request, result, groupProfile);
                            return;
                        }
                    }

                    callback.complete(SocialService.this, request, result, null);
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null);
                }
            }
        });

        Request.Fields _options = new Request.Fields();

        _options.put("profile", groupProfile.toString());
        _options.put("merge", merge ? "true" : "false");
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void updateGroupBatchProfiles(
        LoginService.AccessToken accessToken,
        Map<String, JSONObject> profiles, boolean merge,
        final GroupBatchUpdateCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/groups/profiles",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject groups = response.optJSONObject("groups");
                    if (groups != null)
                    {
                        Map<String, JSONObject> profiles = new HashMap<String, JSONObject>();

                        for (Object o : groups.keySet())
                        {
                            String groupId = o.toString();

                            JSONObject group = groups.optJSONObject(groupId);
                            if (group == null)
                                continue;

                            JSONObject profile = group.optJSONObject("profile");
                            if (profile == null)
                                continue;

                            profiles.put(groupId, profile);
                        }

                        callback.complete(SocialService.this, request, result, profiles);
                        return;
                    }

                    callback.complete(SocialService.this, request, result, null);
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null);
                }
            }
        });

        Request.Fields _options = new Request.Fields();

        JSONObject _profiles = new JSONObject();

        for (String groupId : profiles.keySet())
        {
            _profiles.put(groupId, profiles.get(groupId));
        }

        _options.put("profiles", _profiles.toString());
        _options.put("merge", merge ? "true" : "false");
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void updateGroupSummary(
        LoginService.AccessToken accessToken, String groupId,
        String name,
        Group.JoinMethod joinMethod,
        JSONObject notify,
        final GroupUpdateSummaryCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/group/" + groupId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        if (name != null)
            _options.put("name", name);
        if (joinMethod != null)
            _options.put("join_method", joinMethod.toString());
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void updateMyGroupParticipation(
        LoginService.AccessToken accessToken, String groupId,
        JSONObject participationProfile,
        JSONObject notify,
        boolean merge,
        final GroupUpdateParticipantCallback callback)
    {
        updateGroupParticipation(accessToken, groupId, "me", participationProfile, notify, merge, callback);
    }

    public void updateGroupParticipation(
        LoginService.AccessToken accessToken, String groupId,
        String accountId,
        JSONObject participationProfile,
        JSONObject notify,
        boolean merge,
        final GroupUpdateParticipantCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/group/" + groupId + "/participation/" + accountId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONObject profile = response.optJSONObject("profile");
                    if (profile != null)
                    {
                        callback.complete(SocialService.this, request, result, profile);
                        return;
                    }

                    callback.complete(SocialService.this, request, result, null);
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null);
                }
            }
        });

        Request.Fields _options = new Request.Fields();

        _options.put("profile", participationProfile.toString());
        _options.put("merge", merge ? "true" : "false");
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void updateMyGroupParticipationPermissions(
        LoginService.AccessToken accessToken, String groupId,
        Set<String> permissions,
        int role,
        JSONObject notify,
        final GroupUpdateParticipantPermissionsCallback callback)
    {
        updateGroupParticipationPermissions(accessToken, groupId, "me", permissions, role, notify, callback);
    }

    public void updateGroupParticipationPermissions(
        LoginService.AccessToken accessToken, String groupId,
        String accountId,
        Set<String> permissions,
        int role,
        JSONObject notify,
        final GroupUpdateParticipantPermissionsCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/group/" + groupId + "/participation/" + accountId + "/permissions",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        JSONArray p = new JSONArray();
        for (String permission : permissions)
        {
            p.put(permission);
        }
        _options.put("permissions", p.toString());
        _options.put("role", String.valueOf(role));
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void createGroup(
        LoginService.AccessToken accessToken, String name,
        Group.JoinMethod joinMethod,
        int maxMembers,
        JSONObject groupProfile,
        JSONObject myParticipationProfile,
        boolean enableInGroupMessages,
        final GroupCreateCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/groups/create",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String id = response.optString("id");
                    if (id != null)
                    {
                        callback.complete(SocialService.this, request, result, id);
                        return;
                    }

                    callback.complete(SocialService.this, request, Request.Result.dataCorrupted, null);
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null);
                }
            }
        });

        Request.Fields _options = new Request.Fields();

        _options.put("name", name);
        _options.put("group_profile", groupProfile.toString());
        if (myParticipationProfile != null)
            _options.put("participation_profile", myParticipationProfile.toString());
        _options.put("join_method", joinMethod.toString());
        _options.put("group_messages", enableInGroupMessages ? "true" : "false");
        _options.put("max_members", String.valueOf(maxMembers));
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void searchGroups(
        LoginService.AccessToken accessToken,
        String query,
        final GroupSearchCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/groups/search",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    JSONArray groups = response.optJSONArray("groups");
                    if (groups != null)
                    {
                        LinkedList<Group> out = new LinkedList<Group>();

                        for (int i = 0, t = groups.length(); i < t; i++)
                        {
                            JSONObject g = groups.optJSONObject(i);

                            if (g != null)
                            {
                                out.add(new Group(g));
                            }
                        }

                        callback.complete(SocialService.this, request, result, out);
                        return;
                    }

                    callback.complete(SocialService.this, request, Request.Result.dataCorrupted, null);
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null);
                }
            }
        });

        Request.Fields arguments = new Request.Fields();
        arguments.put("query", query);

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.setQueryArguments(arguments);
        jsonRequest.setToken(accessToken);
        jsonRequest.get();
    }

    public void joinGroup(
        LoginService.AccessToken accessToken, String groupId,
        final GroupJoinCallback callback)
    {
        joinGroup(accessToken, groupId, null, null, callback);
    }

    public void joinGroup(
        LoginService.AccessToken accessToken,
        String groupId,
        JSONObject participationProfile,
        JSONObject notify,
        final GroupJoinCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/group/" + groupId + "/join",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        if (participationProfile != null)
            _options.put("participation_profile", participationProfile.toString());
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void acceptGroupInvitation(
        LoginService.AccessToken accessToken,
        String groupId,
        JSONObject participationProfile,
        JSONObject notify, String key,
        final GroupJoinCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/group/" + groupId + "/invitation/accept",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        _options.put("participation_profile", participationProfile.toString());
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("key", key);
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void rejectGroupInvitation(
        LoginService.AccessToken accessToken,
        String groupId,
        JSONObject notify,
        String key,
        final GroupJoinCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/group/" + groupId + "/invitation/reject",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("key", key);
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void leaveGroup(
        LoginService.AccessToken accessToken,
        String groupId,
        final GroupLeaveCallback callback)
    {
        leaveGroup(accessToken, groupId, null, callback);
    }

    public void leaveGroup(
        LoginService.AccessToken accessToken, String groupId,
        JSONObject notify,
        final GroupLeaveCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/group/" + groupId + "/leave",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void kickFromGroup(
        LoginService.AccessToken accessToken,
        String groupId,
        String accountId,
        final GroupKickCallback callback)
    {
        kickFromGroup(accessToken, groupId, accountId, null, callback);
    }

    public void kickFromGroup(
        LoginService.AccessToken accessToken,
        String groupId,
        String accountId,
        JSONObject notify,
        final GroupKickCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/group/" + groupId + "/participation/" + accountId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.delete(_options);
    }

    public void transferOwnership(
        LoginService.AccessToken accessToken,
        String groupId,
        String accountTransferTo,
        int myNewRole,
        JSONObject notify,
        final GroupJoinCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/group/" + groupId + "/ownership",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("account_transfer_to", accountTransferTo);
        _options.put("my_role", String.valueOf(myNewRole));
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void requestJoinGroup(
        LoginService.AccessToken accessToken, String groupId,
        JSONObject participationProfile,
        JSONObject notify,
        final GroupJoinRequestCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(getLocation() + "/group/" + groupId + "/request",
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String key = response.optString("key");
                    callback.complete(SocialService.this, request, result, key);
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null);
                }
            }
        });

        Request.Fields _options = new Request.Fields();

        if (participationProfile != null)
            _options.put("participation_profile", participationProfile.toString());
        if (notify != null)
            _options.put("notify", notify.toString());
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void inviteToGroup(
        LoginService.AccessToken accessToken, String groupId,
        String accountId,
        int role,
        final GroupInviteCallback callback)
    {
        inviteToGroup(accessToken, groupId, accountId, role, null, null, callback);
    }

    public void inviteToGroup(
        LoginService.AccessToken accessToken, String groupId,
        String accountId,
        int role,
        Set<String> permissions,
        JSONObject notify,
        final GroupInviteCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/group/" + groupId + "/invite/" + accountId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                if (result == Request.Result.success)
                {
                    JSONObject response = ((JsonRequest) request).getObject();

                    String key = response.optString("key");
                    if (key != null)
                    {
                        callback.complete(SocialService.this, request, result, key);
                        return;
                    }

                    callback.complete(SocialService.this, request, result, null);
                }
                else
                {
                    callback.complete(SocialService.this, request, result, null);
                }
            }
        });

        Request.Fields _options = new Request.Fields();

        _options.put("role", String.valueOf(role));
        if (permissions != null)
        {
            JSONArray p = new JSONArray();
            for (String permission : permissions)
            {
                p.put(permission);
            }
            _options.put("permissions", p.toString());
        }
        else
        {
            _options.put("permissions", "[]");
        }
        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void approveJoin(
        LoginService.AccessToken accessToken,
        String groupId,
        String accountId,
        String key,
        int role,
        Set<String> permissions,
        JSONObject notify,
        final GroupJoinApproveCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/group/" + groupId + "/approve/" + accountId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        _options.put("role", String.valueOf(role));
        if (permissions != null)
        {
            JSONArray p = new JSONArray();
            for (String permission : permissions)
            {
                p.put(permission);
            }
            _options.put("permissions", p.toString());
        }
        else
        {
            _options.put("permissions", "[]");
        }

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("key", key);
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }

    public void rejectJoin(
        LoginService.AccessToken accessToken,
        String groupId,
        String accountId,
        String key,
        JSONObject notify,
        final GroupJoinApproveCallback callback)
    {
        JsonRequest jsonRequest = new JsonRequest(
                getLocation() + "/group/" + groupId + "/reject/" + accountId,
            new Request.RequestCallback()
        {
            @Override
            public void complete(Request request, Request.Result result)
            {
                callback.complete(SocialService.this, request, result);
            }
        });

        Request.Fields _options = new Request.Fields();

        if (notify != null)
            _options.put("notify", notify.toString());

        _options.put("key", key);
        _options.put("access_token", accessToken.toString());

        jsonRequest.setAPIVersion(getAPIVersion());
        jsonRequest.post(_options);
    }
}
