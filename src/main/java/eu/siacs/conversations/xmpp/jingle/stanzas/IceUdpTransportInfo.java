package eu.siacs.conversations.xmpp.jingle.stanzas;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.xml.Element;
import eu.siacs.conversations.xml.Namespace;
import eu.siacs.conversations.xmpp.jingle.SessionDescription;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class IceUdpTransportInfo extends GenericTransportInfo {

    public static final IceUdpTransportInfo STUB = new IceUdpTransportInfo();

    public IceUdpTransportInfo() {
        super("transport", Namespace.JINGLE_TRANSPORT_ICE_UDP);
    }

    public static IceUdpTransportInfo upgrade(final Element element) {
        Preconditions.checkArgument(
                "transport".equals(element.getName()), "Name of provided element is not transport");
        Preconditions.checkArgument(
                Namespace.JINGLE_TRANSPORT_ICE_UDP.equals(element.getNamespace()),
                "Element does not match ice-udp transport namespace");
        final IceUdpTransportInfo transportInfo = new IceUdpTransportInfo();
        transportInfo.setAttributes(element.getAttributes());
        transportInfo.setChildren(element.getChildren());
        return transportInfo;
    }

    public static IceUdpTransportInfo of(
            SessionDescription sessionDescription, SessionDescription.Media media) {
        final String ufrag = Iterables.getFirst(media.attributes.get("ice-ufrag"), null);
        final String pwd = Iterables.getFirst(media.attributes.get("ice-pwd"), null);
        final IceUdpTransportInfo iceUdpTransportInfo = new IceUdpTransportInfo();
        if (ufrag != null) {
            iceUdpTransportInfo.setAttribute("ufrag", ufrag);
        }
        if (pwd != null) {
            iceUdpTransportInfo.setAttribute("pwd", pwd);
        }
        final Fingerprint fingerprint = Fingerprint.of(sessionDescription, media);
        if (fingerprint != null) {
            iceUdpTransportInfo.addChild(fingerprint);
        }
        for (final String iceOption : IceOption.of(media)) {
            iceUdpTransportInfo.addChild(new IceOption(iceOption));
        }
        return iceUdpTransportInfo;
    }

    public static IceUdpTransportInfo of(
            final Credentials credentials,
            final Collection<String> iceOptions,
            final Setup setup,
            final String hash,
            final String fingerprint) {
        final IceUdpTransportInfo iceUdpTransportInfo = new IceUdpTransportInfo();
        iceUdpTransportInfo.addChild(Fingerprint.of(setup, hash, fingerprint));
        iceUdpTransportInfo.setAttribute("ufrag", credentials.ufrag);
        iceUdpTransportInfo.setAttribute("pwd", credentials.password);
        for (final String iceOption : iceOptions) {
            iceUdpTransportInfo.addChild(new IceOption(iceOption));
        }
        return iceUdpTransportInfo;
    }

    public Fingerprint getFingerprint() {
        final Element fingerprint = this.findChild("fingerprint", Namespace.JINGLE_APPS_DTLS);
        return fingerprint == null ? null : Fingerprint.upgrade(fingerprint);
    }

    public List<String> getIceOptions() {
        final ImmutableList.Builder<String> optionBuilder = new ImmutableList.Builder<>();
        for (final Element child : this.children) {
            if (Namespace.JINGLE_TRANSPORT_ICE_OPTION.equals(child.getNamespace())
                    && IceOption.WELL_KNOWN.contains(child.getName())) {
                optionBuilder.add(child.getName());
            }
        }
        return optionBuilder.build();
    }

    public Credentials getCredentials() {
        final String ufrag = this.getAttribute("ufrag");
        final String password = this.getAttribute("pwd");
        return new Credentials(ufrag, password);
    }

    public boolean isStub() {
        return Strings.isNullOrEmpty(this.getAttribute("ufrag"))
                && Strings.isNullOrEmpty(this.getAttribute("pwd"))
                && this.children.isEmpty();
    }

    public List<Candidate> getCandidates() {
        final ImmutableList.Builder<Candidate> builder = new ImmutableList.Builder<>();
        for (final Element child : getChildren()) {
            if ("candidate".equals(child.getName())) {
                builder.add(Candidate.upgrade(child));
            }
        }
        return builder.build();
    }

    public IceUdpTransportInfo cloneWrapper() {
        final IceUdpTransportInfo transportInfo = new IceUdpTransportInfo();
        transportInfo.setAttributes(new Hashtable<>(getAttributes()));
        return transportInfo;
    }

    public IceUdpTransportInfo modifyCredentials(final Credentials credentials, final Setup setup) {
        final IceUdpTransportInfo transportInfo = new IceUdpTransportInfo();
        transportInfo.setAttribute("ufrag", credentials.ufrag);
        transportInfo.setAttribute("pwd", credentials.password);
        for (final Element child : getChildren()) {
            if (child.getName().equals("fingerprint")
                    && Namespace.JINGLE_APPS_DTLS.equals(child.getNamespace())) {
                final Fingerprint fingerprint = new Fingerprint();
                fingerprint.setAttributes(new Hashtable<>(child.getAttributes()));
                fingerprint.setContent(child.getContent());
                fingerprint.setAttribute("setup", setup.toString().toLowerCase(Locale.ROOT));
                transportInfo.addChild(fingerprint);
            }
        }
        for (final String iceOption : this.getIceOptions()) {
            transportInfo.addChild(new IceOption(iceOption));
        }
        return transportInfo;
    }

    public static class Credentials {
        public final String ufrag;
        public final String password;

        public Credentials(String ufrag, String password) {
            this.ufrag = ufrag;
            this.password = password;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Credentials that = (Credentials) o;
            return Objects.equal(ufrag, that.ufrag) && Objects.equal(password, that.password);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(ufrag, password);
        }

        @Override
        @NonNull
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("ufrag", ufrag)
                    .add("password", password)
                    .toString();
        }
    }

    public static class Candidate extends Element {

        private Candidate() {
            super("candidate");
        }

        public static Candidate upgrade(final Element element) {
            Preconditions.checkArgument("candidate".equals(element.getName()));
            final Candidate candidate = new Candidate();
            candidate.setAttributes(element.getAttributes());
            candidate.setChildren(element.getChildren());
            return candidate;
        }

        // https://tools.ietf.org/html/draft-ietf-mmusic-ice-sip-sdp-39#section-5.1
        public static Candidate fromSdpAttribute(final String attribute, String currentUfrag) {
            final String[] pair = attribute.split(":", 2);
            if (pair.length == 2 && "candidate".equals(pair[0])) {
                return fromSdpAttributeValue(pair[1], currentUfrag);
            }
            return null;
        }

        public static Candidate fromSdpAttributeValue(final String value, final String currentUfrag) {
            final String[] segments = value.split(" ");
            if (segments.length < 6) {
                return null;
            }
            final String id = UUID.randomUUID().toString();
            final String foundation = segments[0];
            final String component = segments[1];
            final String transport = segments[2].toLowerCase(Locale.ROOT);
            final String priority = segments[3];
            final String connectionAddress = segments[4];
            final String port = segments[5];
            final HashMap<String, String> additional = new HashMap<>();
            for (int i = 6; i < segments.length - 1; i = i + 2) {
                additional.put(segments[i], segments[i + 1]);
            }
            final String ufrag = additional.get("ufrag");
            if (currentUfrag != null && ufrag != null && !ufrag.equals(currentUfrag)) {
                return null;
            }
            final Candidate candidate = new Candidate();
            candidate.setAttribute("component", component);
            candidate.setAttribute("foundation", foundation);
            candidate.setAttribute("generation", additional.get("generation"));
            candidate.setAttribute("rel-addr", additional.get("raddr"));
            candidate.setAttribute("rel-port", additional.get("rport"));
            candidate.setAttribute("id", id);
            candidate.setAttribute("ip", connectionAddress);
            candidate.setAttribute("port", port);
            candidate.setAttribute("priority", priority);
            candidate.setAttribute("protocol", transport);
            candidate.setAttribute("type", additional.get("typ"));
            return candidate;
        }

        public int getComponent() {
            return getAttributeAsInt("component");
        }

        public int getFoundation() {
            return getAttributeAsInt("foundation");
        }

        public int getGeneration() {
            return getAttributeAsInt("generation");
        }

        public String getId() {
            return getAttribute("id");
        }

        public String getIp() {
            return getAttribute("ip");
        }

        public int getNetwork() {
            return getAttributeAsInt("network");
        }

        public int getPort() {
            return getAttributeAsInt("port");
        }

        public int getPriority() {
            return getAttributeAsInt("priority");
        }

        public String getProtocol() {
            return getAttribute("protocol");
        }

        public String getRelAddr() {
            return getAttribute("rel-addr");
        }

        public int getRelPort() {
            return getAttributeAsInt("rel-port");
        }

        public String getType() { // TODO might be converted to enum
            return getAttribute("type");
        }

        private int getAttributeAsInt(final String name) {
            final String value = this.getAttribute(name);
            if (value == null) {
                return 0;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public String toSdpAttribute(final String ufrag) {
            final String foundation = this.getAttribute("foundation");
            checkNotNullNoWhitespace(foundation, "foundation");
            final String component = this.getAttribute("component");
            checkNotNullNoWhitespace(component, "component");
            final String protocol = this.getAttribute("protocol");
            checkNotNullNoWhitespace(protocol, "protocol");
            final String transport = protocol.toLowerCase(Locale.ROOT);
            if (!"udp".equals(transport)) {
                throw new IllegalArgumentException(
                        String.format("'%s' is not a supported protocol", transport));
            }
            final String priority = this.getAttribute("priority");
            checkNotNullNoWhitespace(priority, "priority");
            final String connectionAddress = this.getAttribute("ip");
            checkNotNullNoWhitespace(connectionAddress, "ip");
            final String port = this.getAttribute("port");
            checkNotNullNoWhitespace(port, "port");
            final Map<String, String> additionalParameter = new LinkedHashMap<>();
            final String relAddr = this.getAttribute("rel-addr");
            final String type = this.getAttribute("type");
            if (type != null) {
                additionalParameter.put("typ", type);
            }
            if (relAddr != null) {
                additionalParameter.put("raddr", relAddr);
            }
            final String relPort = this.getAttribute("rel-port");
            if (relPort != null) {
                additionalParameter.put("rport", relPort);
            }
            final String generation = this.getAttribute("generation");
            if (generation != null) {
                additionalParameter.put("generation", generation);
            }
            if (ufrag != null) {
                additionalParameter.put("ufrag", ufrag);
            }
            final String parametersString =
                    Joiner.on(' ')
                            .join(
                                    Collections2.transform(
                                            additionalParameter.entrySet(),
                                            input ->
                                                    String.format(
                                                            "%s %s",
                                                            input.getKey(), input.getValue())));
            return String.format(
                    "candidate:%s %s %s %s %s %s %s",
                    foundation,
                    component,
                    transport,
                    priority,
                    connectionAddress,
                    port,
                    parametersString);
        }
    }

    private static void checkNotNullNoWhitespace(final String value, final String name) {
        if (Strings.isNullOrEmpty(value)) {
            throw new IllegalArgumentException(
                    String.format("Parameter %s is missing or empty", name));
        }
        SessionDescription.checkNoWhitespace(
                value, String.format("Parameter %s contains white spaces", name));
    }

    public static class Fingerprint extends Element {

        private Fingerprint() {
            super("fingerprint", Namespace.JINGLE_APPS_DTLS);
        }

        public static Fingerprint upgrade(final Element element) {
            Preconditions.checkArgument("fingerprint".equals(element.getName()));
            Preconditions.checkArgument(Namespace.JINGLE_APPS_DTLS.equals(element.getNamespace()));
            final Fingerprint fingerprint = new Fingerprint();
            fingerprint.setAttributes(element.getAttributes());
            fingerprint.setContent(element.getContent());
            return fingerprint;
        }

        private static Fingerprint of(ArrayListMultimap<String, String> attributes) {
            final String fingerprint = Iterables.getFirst(attributes.get("fingerprint"), null);
            final String setup = Iterables.getFirst(attributes.get("setup"), null);
            if (setup != null && fingerprint != null) {
                final String[] fingerprintParts = fingerprint.split(" ", 2);
                if (fingerprintParts.length == 2) {
                    final String hash = fingerprintParts[0];
                    final String actualFingerprint = fingerprintParts[1];
                    final Fingerprint element = new Fingerprint();
                    element.setAttribute("hash", hash);
                    element.setAttribute("setup", setup);
                    element.setContent(actualFingerprint);
                    return element;
                }
            }
            return null;
        }

        public static Fingerprint of(
                final SessionDescription sessionDescription, final SessionDescription.Media media) {
            final Fingerprint fingerprint = of(media.attributes);
            return fingerprint == null ? of(sessionDescription.attributes) : fingerprint;
        }

        private static Fingerprint of(final Setup setup, final String hash, final String content) {
            final Fingerprint fingerprint = new Fingerprint();
            fingerprint.setContent(content);
            fingerprint.setAttribute("hash", hash);
            fingerprint.setAttribute("setup", setup.toString().toLowerCase(Locale.ROOT));
            return fingerprint;
        }

        public String getHash() {
            return this.getAttribute("hash");
        }

        public Setup getSetup() {
            final String setup = this.getAttribute("setup");
            return setup == null ? null : Setup.of(setup);
        }
    }

    public enum Setup {
        ACTPASS,
        PASSIVE,
        ACTIVE;

        public static Setup of(String setup) {
            try {
                return valueOf(setup.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        public Setup flip() {
            if (this == PASSIVE) {
                return ACTIVE;
            }
            if (this == ACTIVE) {
                return PASSIVE;
            }
            throw new IllegalStateException(this.name() + " can not be flipped");
        }
    }

    public static class IceOption extends Element {

        public static final List<String> WELL_KNOWN = Arrays.asList("trickle", "renomination");

        public IceOption(final String name) {
            super(name, Namespace.JINGLE_TRANSPORT_ICE_OPTION);
        }

        public static Collection<String> of(SessionDescription.Media media) {
            final String iceOptions = Iterables.getFirst(media.attributes.get("ice-options"), null);
            if (Strings.isNullOrEmpty(iceOptions)) {
                return Collections.emptyList();
            }
            final ImmutableList.Builder<String> optionBuilder = new ImmutableList.Builder<>();
            for (final String iceOption : Splitter.on(' ').split(iceOptions)) {
                if (WELL_KNOWN.contains(iceOption)) {
                    optionBuilder.add(iceOption);
                } else {
                    Log.w(Config.LOGTAG, "unrecognized ice option: " + iceOption);
                }
            }
            return optionBuilder.build();
        }
    }
}
