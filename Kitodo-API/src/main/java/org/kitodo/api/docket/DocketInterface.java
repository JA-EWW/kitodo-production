/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.api.docket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

public interface DocketInterface {

    /**
     * Generates a docket from given data.
     *
     * @param docketData
     *            - the data shown in the docket
     * @param xslFileUri
     *            - the uri to the schema xsl file
     * @return a docket file
     */
    OutputStream generateDocket(DocketData docketData, URI xslFileUri) throws IOException;

    /**
     * Generates multiple dockets
     *
     * @param docketData
     *            - a List data shown in the dockets
     * @param xslFileUri
     *            - the uri to the schema xsl file
     * @return a list of docket files.
     */
    OutputStream generateMultipleDockets(ArrayList<DocketData> docketData, URI xslFileUri) throws IOException;

}
