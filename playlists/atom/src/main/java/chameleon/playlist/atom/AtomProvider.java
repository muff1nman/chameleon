/*
 * Copyright (c) 2008, Christophe Delory
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY CHRISTOPHE DELORY ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CHRISTOPHE DELORY BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package chameleon.playlist.atom;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.Date;

import chameleon.Chameleon;
import org.apache.commons.logging.Log;

import chameleon.atom.Entry;
import chameleon.atom.Feed;
import chameleon.atom.Generator;
import chameleon.atom.Link;
import chameleon.atom.Person;
import chameleon.atom.TextContainer;
import chameleon.atom.URIContainer;
import chameleon.content.type.ContentType;
import chameleon.io.IOUtils;
import chameleon.player.PlayerSupport;
import chameleon.playlist.AbstractPlaylistComponent;
import chameleon.playlist.Media;
import chameleon.playlist.Parallel;
import chameleon.playlist.Playlist;
import chameleon.playlist.Sequence;
import chameleon.playlist.SpecificPlaylist;
import chameleon.playlist.SpecificPlaylistProvider;
import chameleon.xml.Version;
import chameleon.xml.XmlSerializer;

/**
 * The Atom playlist provider.
 * @version $Revision: 91 $
 * @author Christophe Delory
 */
public class AtomProvider implements SpecificPlaylistProvider
{
    /**
     * A list of compatible content types.
     */
    private static final ContentType[] FILETYPES =
    {
        new ContentType(new String[] { ".atom", ".xml" },
                        new String[] { "application/atom+xml" },
                        new PlayerSupport[]
                        {
                        },
                        "Atom Document"),
    };

    @Override
    public String getId()
    {
        return "atom";
    }

    @Override
    public ContentType[] getContentTypes()
    {
        return FILETYPES.clone();
    }

    @Override
    public SpecificPlaylist readFrom(final InputStream in, final String encoding, final Log logger) throws Exception
    {
        String enc = encoding;

        if (enc == null)
        {
            enc = "UTF-8";
        }

        String str = IOUtils.toString(in, enc); // May throw IOException. Throws NullPointerException if in is null.

        // Replace all occurrences of a single '&' with "&amp;" (or leave this construct as is).
        // First replace blindly all '&' to its corresponding character reference.
        str = str.replace("&", "&amp;");
        // Then restore any existing character reference.
        str = str.replaceAll("&amp;([a-zA-Z0-9#]+;)", "&$1"); // Shall not throw PatternSyntaxException.

        // Workaround Castor bug 2521:
        str = str.replace("xmlns=\"http://www.w3.org/2005/Atom\"", "");

        // Unmarshal the SMIL playlist.
        final XmlSerializer serializer = XmlSerializer.getMapping("chameleon/atom"); // May throw Exception.
        serializer.getUnmarshaller().setIgnoreExtraElements(true);

        final StringReader reader = new StringReader(str);
        // TODO Allow also an Entry.
        final Feed feed = (Feed) serializer.unmarshal(reader); // May throw Exception.

        final AtomPlaylist ret = new AtomPlaylist();
        ret.setProvider(this);
        ret.setFeed(feed);

        return ret;
    }

    @Override
    public SpecificPlaylist toSpecificPlaylist(final Playlist playlist) throws Exception
    {
        final AtomPlaylist ret = new AtomPlaylist();
        ret.setProvider(this);

        final Feed feed = ret.getFeed();

        final TextContainer title = new TextContainer();
        title.setText("Chamelon v" + Version.CURRENT + " Atom playlist");
        feed.setTitle(title);
        feed.setUpdated(new Date());

        final URIContainer id = new URIContainer();
        final StringBuilder sb = new StringBuilder();
        sb.append("urn:uuid:");
        final String tmpId = Integer.toHexString(System.identityHashCode(feed));
        for (int i = tmpId.length(); i < 8; i++)
        {
            sb.append('0');
        }
        sb.append(tmpId);
        sb.append("-d399-11d9-b93C-0003939e0af6");
        id.setURIString(sb.toString());
        feed.setId(id);

        final Generator generator = new Generator();
        generator.setValue(Chameleon.NAME);
        generator.setVersion(Version.CURRENT.toString());
        generator.setURIString(Chameleon.URL);
        feed.setGenerator(generator);

        final Person me = new Person();
        me.setName(Chameleon.AUTHOR);
        me.setURIString(Chameleon.URL);
        me.setEmail(Chameleon.CONTACT);
        feed.addContributor(me);

        addToPlaylist(feed, playlist.getRootSequence()); // May throw Exception

        return ret;
    }

    /**
     * Adds the specified generic playlist component, and all its childs if any, to the input Atom feed.
     * @param feed the parent Atom feed. Shall not be <code>null</code>.
     * @param component the generic playlist component to handle. Shall not be <code>null</code>.
     * @throws NullPointerException if <code>feed</code> is <code>null</code>.
     * @throws NullPointerException if <code>component</code> is <code>null</code>.
     * @throws Exception if this service provider is unable to represent the input playlist.
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void addToPlaylist(final Feed feed, final AbstractPlaylistComponent component) throws Exception
    {
        if (component instanceof Sequence)
        {
            final Sequence sequence = (Sequence) component;

            if (sequence.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("An Atom playlist cannot handle a sequence repeated indefinitely");
            }

            final AbstractPlaylistComponent[] components = sequence.getComponents();

            for (int iter = 0; iter < sequence.getRepeatCount(); iter++)
            {
                for (AbstractPlaylistComponent c : components)
                {
                    addToPlaylist(feed, c); // May throw Exception.
                }
            }
        }
        else if (component instanceof Parallel)
        {
            throw new IllegalArgumentException("An Atom playlist doesn't support concurrent media");
        }
        else if (component instanceof Media)
        {
            final Media media = (Media) component;

            if (media.getDuration() != null)
            {
                throw new IllegalArgumentException("An Atom playlist cannot handle a timed media");
            }

            if (media.getRepeatCount() < 0)
            {
                throw new IllegalArgumentException("An Atom playlist cannot handle a media repeated indefinitely");
            }

            if (media.getSource() != null)
            {
                for (int iter = 0; iter < media.getRepeatCount(); iter++)
                {
                    final Entry entry = new Entry();
                    final Link link = new Link();
                    final URI uri = media.getSource().getURI(); // May throw SecurityException, URISyntaxException.
                    link.setHref(uri.toString());
                    link.setRel("enclosure");
                    link.setType(media.getSource().getType());

                    if (media.getSource().getLength() >= 0L) // NOPMD Deeply nested if..then statements are hard to read
                    {
                        link.setLength(Long.valueOf(media.getSource().getLength()));
                    }

                    entry.addLink(link);

                    final TextContainer title = new TextContainer();

                    if (uri.getPath() == null)
                    {
                        title.setText(media.getSource().toString());
                    }
                    else
                    {
                        final File path = new File(uri.getPath());
                        title.setText(path.getName());
                    }

                    entry.setTitle(title);

                    if (media.getSource().getLastModified() > 0L)
                    {
                        entry.setUpdated(new Date(media.getSource().getLastModified()));
                    }
                    else
                    {
                        entry.setUpdated(new Date());
                    }

                    entry.setPublished(new Date());

                    final URIContainer id = new URIContainer();
                    final StringBuilder sb = new StringBuilder();
                    sb.append("urn:uuid:");
                    final String tmpId = Integer.toHexString(System.identityHashCode(entry));
                    for (int i = tmpId.length(); i < 8; i++)
                    {
                        sb.append('0');
                    }
                    sb.append(tmpId);
                    sb.append("-d399-11d9-b93C-0003939e0af6");
                    id.setURIString(sb.toString());
                    entry.setId(id);

                    feed.addEntry(entry);
                }
            }
        }
    }
}
