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

package de.sub.goobi.export.dms;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.config.ConfigProjects;
import de.sub.goobi.export.download.ExportMets;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.tasks.EmptyTask;
import de.sub.goobi.helper.tasks.ExportDmsTask;
import de.sub.goobi.helper.tasks.TaskManager;
import de.sub.goobi.helper.tasks.TaskSitter;
import de.sub.goobi.metadaten.MetadatenVerifizierung;
import de.sub.goobi.metadaten.copier.CopierData;
import de.sub.goobi.metadaten.copier.DataCopier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.exceptions.SwapException;
import org.kitodo.data.database.helper.enums.MetadataFormat;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.file.FileService;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;
import ugh.fileformats.excel.RDFFile;
import ugh.fileformats.mets.MetsModsImportExport;

public class ExportDms extends ExportMets {
    private static final Logger logger = LogManager.getLogger(ExportDms.class);
    ConfigProjects cp;
    private boolean exportWithImages = true;
    private boolean exportFullText = true;
    private final ServiceManager serviceManager = new ServiceManager();
    private final FileService fileService = serviceManager.getFileService();

    /**
     * The field exportDmsTask holds an optional task instance. Its progress and
     * its errors will be passed to the task manager screen (if available) for
     * visualisation.
     */
    public EmptyTask exportDmsTask = null;

    public static final String DIRECTORY_SUFFIX = "_tif";

    public ExportDms() {
    }

    public ExportDms(boolean exportImages) {
        this.exportWithImages = exportImages;
    }

    public void setExportFullText(boolean exportFullText) {
        this.exportFullText = exportFullText;
    }

    /**
     * DMS-Export an eine gewünschte Stelle.
     *
     * @param process
     *            object
     * @param inZielVerzeichnis
     *            String
     */
    @Override
    public boolean startExport(Process process, String inZielVerzeichnis) throws IOException, InterruptedException,
            WriteException, PreferencesException, SwapException, DAOException, TypeNotAllowedForParentException {

        Hibernate.initialize(process.getProject().getProjectFileGroups());
        if (process.getProject().isUseDmsImport()
                && ConfigCore.getBooleanParameter("asynchronousAutomaticExport", false)) {
            Hibernate.initialize(process.getRuleset());
            TaskManager.addTask(new ExportDmsTask(this, process, inZielVerzeichnis));
            Helper.setMeldung(TaskSitter.isAutoRunningThreads() ? "DMSExportByThread" : "DMSExportThreadCreated",
                    process.getTitle());
            return true;
        } else {
            return startExport(process, inZielVerzeichnis, (ExportDmsTask) null);
        }
    }

    /**
     * The function startExport() performs a DMS export to a desired place. In
     * addition, it accepts an optional ExportDmsTask object. If that is passed
     * in, the progress in it will be updated during processing and occurring
     * errors will be passed to it to be visible in the task manager screen.
     *
     * @param process
     *            process to export
     * @param inZielVerzeichnis
     *            work directory of the user who triggered the export
     * @param exportDmsTask
     *            ExportDmsTask object to submit progress updates and errors
     * @return false if an error condition was caught, true otherwise
     * @throws IOException
     *             if “kitodo_projects.xml” could not be read
     * @throws InterruptedException
     *             if the thread running the script to create a directory is
     *             interrupted by another thread while it is waiting
     * @throws WriteException
     *             if a FileNotFoundException occurs when opening the
     *             FileOutputStream to write the METS/MODS object
     * @throws PreferencesException
     *             if the file format selected for DMS export in the project of
     *             the process to export that implements
     *             {@link ugh.dl.Fileformat#getDigitalDocument()} throws it
     * @throws SwapException
     *             if after swapping a process back in neither a file system
     *             entry "images" nor "meta.xml" exists
     * @throws DAOException
     *             if saving the fact that a process has been swapped back in to
     *             the database fails
     * @throws TypeNotAllowedForParentException
     *             declared in
     *             {@link ugh.dl.DigitalDocument#createDocStruct(DocStructType)}
     *             but never thrown, see
     *             https://github.com/kitodo/kitodo-ugh/issues/2
     */
    public boolean startExport(Process process, String inZielVerzeichnis, ExportDmsTask exportDmsTask)
            throws IOException, InterruptedException, WriteException, PreferencesException, SwapException, DAOException,
            TypeNotAllowedForParentException {
        this.exportDmsTask = exportDmsTask;
        try {
            return startExport(process, inZielVerzeichnis,
                    serviceManager.getProcessService().readMetadataFile(process).getDigitalDocument());
        } catch (Exception e) {
            if (exportDmsTask != null) {
                exportDmsTask.setException(e);
            } else {
                Helper.setFehlerMeldung(Helper.getTranslation("exportError") + process.getTitle(), e);
            }
            logger.error("Export abgebrochen, xml-LeseFehler", e);
            return false;
        }
    }

    /**
     * Start export.
     *
     * @param process
     *            object
     * @param inZielVerzeichnis
     *            String
     * @param newFile
     *            DigitalDocument
     * @return boolean
     */
    public boolean startExport(Process process, String inZielVerzeichnis, DigitalDocument newFile)
            throws IOException, InterruptedException, WriteException, PreferencesException, SwapException, DAOException,
            TypeNotAllowedForParentException {

        this.myPrefs = serviceManager.getRulesetService().getPreferences(process.getRuleset());
        this.cp = new ConfigProjects(process.getProject().getTitle());
        String atsPpnBand = process.getTitle();

        /*
         * Dokument einlesen
         */
        Fileformat gdzfile;
        try {
            switch (MetadataFormat.findFileFormatsHelperByName(process.getProject().getFileFormatDmsExport())) {
                case METS:
                    gdzfile = new MetsModsImportExport(this.myPrefs);
                    break;

                case METS_AND_RDF:
                default:
                    gdzfile = new RDFFile(this.myPrefs);
                    break;
            }

            gdzfile.setDigitalDocument(newFile);

        } catch (Exception e) {
            if (exportDmsTask != null) {
                exportDmsTask.setException(e);
            } else {
                Helper.setFehlerMeldung(Helper.getTranslation("exportError") + process.getTitle(), e);
            }
            logger.error("Export abgebrochen, xml-LeseFehler", e);
            return false;
        }

        String rules = ConfigCore.getParameter("copyData.onExport");
        if (rules != null && !rules.equals("- keine Konfiguration gefunden -")) {
            try {
                new DataCopier(rules).process(new CopierData(gdzfile, process));
            } catch (ConfigurationException e) {
                if (exportDmsTask != null) {
                    exportDmsTask.setException(e);
                } else {
                    Helper.setFehlerMeldung("dataCopier.syntaxError", e.getMessage());
                }
                return false;
            } catch (RuntimeException e) {
                if (exportDmsTask != null) {
                    exportDmsTask.setException(e);
                } else {
                    Helper.setFehlerMeldung("dataCopier.runtimeException", e.getMessage());
                }
                return false;
            }
        }

        trimAllMetadata(gdzfile.getDigitalDocument().getLogicalDocStruct());

        /*
         * Metadaten validieren
         */

        if (ConfigCore.getBooleanParameter("useMetadatenvalidierung")) {
            MetadatenVerifizierung mv = new MetadatenVerifizierung();
            if (!mv.validate(gdzfile, this.myPrefs, process)) {
                return false;
            }
        }

        /*
         * Speicherort vorbereiten und downloaden
         */
        String zielVerzeichnis;
        File userHome;
        if (process.getProject().isUseDmsImport()) {
            zielVerzeichnis = process.getProject().getDmsImportImagesPath();
            userHome = new File(zielVerzeichnis);

            /* ggf. noch einen Vorgangsordner anlegen */
            if (process.getProject().isDmsImportCreateProcessFolder()) {
                userHome = new File(userHome + File.separator + process.getTitle());
                zielVerzeichnis = userHome.getAbsolutePath();
                /* alte Import-Ordner löschen */
                if (!fileService.delete(userHome.toURI())) {
                    Helper.setFehlerMeldung("Export canceled, Process: " + process.getTitle(),
                            "Import folder could not be cleared");
                    return false;
                }
                /* alte Success-Ordner löschen */
                File successFile = new File(
                        process.getProject().getDmsImportSuccessPath() + File.separator + process.getTitle());
                if (!fileService.delete(successFile.toURI())) {
                    Helper.setFehlerMeldung("Export canceled, Process: " + process.getTitle(),
                            "Success folder could not be cleared");
                    return false;
                }
                /* alte Error-Ordner löschen */
                File errorfile = new File(
                        process.getProject().getDmsImportErrorPath() + File.separator + process.getTitle());
                if (!fileService.delete(errorfile.toURI())) {
                    Helper.setFehlerMeldung("Export canceled, Process: " + process.getTitle(),
                            "Error folder could not be cleared");
                    return false;
                }

                if (!userHome.exists()) {
                    userHome.mkdir();
                }
            }

        } else {
            zielVerzeichnis = inZielVerzeichnis + atsPpnBand + File.separator;
            // wenn das Home existiert, erst löschen und dann neu anlegen
            userHome = new File(zielVerzeichnis);
            if (!fileService.delete(userHome.toURI())) {
                Helper.setFehlerMeldung("Export canceled: " + process.getTitle(), "could not delete home directory");
                return false;
            }
            prepareUserDirectory(zielVerzeichnis);
        }
        if (exportDmsTask != null) {
            exportDmsTask.setProgress(1);
        }

        /*
         * der eigentliche Download der Images
         */
        try {
            if (this.exportWithImages) {
                imageDownload(process, userHome, atsPpnBand, DIRECTORY_SUFFIX);
                fulltextDownload(process, userHome, atsPpnBand, DIRECTORY_SUFFIX);
            } else if (this.exportFullText) {
                fulltextDownload(process, userHome, atsPpnBand, DIRECTORY_SUFFIX);
            }
            directoryDownload(process, zielVerzeichnis);
        } catch (Exception e) {
            if (exportDmsTask != null) {
                exportDmsTask.setException(e);
            } else {
                Helper.setFehlerMeldung("Export canceled, Process: " + process.getTitle(), e);
            }
            return false;
        }

        /*
         * zum Schluss Datei an gewünschten Ort exportieren entweder direkt in
         * den Import-Ordner oder ins Benutzerhome anschliessend den
         * Import-Thread starten
         */
        if (process.getProject().isUseDmsImport()) {
            if (exportDmsTask != null) {
                exportDmsTask.setWorkDetail(atsPpnBand + ".xml");
            }
            if (MetadataFormat.findFileFormatsHelperByName(
                    process.getProject().getFileFormatDmsExport()) == MetadataFormat.METS) {
                /* Wenn METS, dann per writeMetsFile schreiben... */
                writeMetsFile(process, userHome + File.separator + atsPpnBand + ".xml", gdzfile, false);
            } else {
                /* ...wenn nicht, nur ein Fileformat schreiben. */
                gdzfile.write(userHome + File.separator + atsPpnBand + ".xml");
            }

            /* ggf. sollen im Export mets und rdf geschrieben werden */
            if (MetadataFormat.findFileFormatsHelperByName(
                    process.getProject().getFileFormatDmsExport()) == MetadataFormat.METS_AND_RDF) {
                writeMetsFile(process, userHome + File.separator + atsPpnBand + ".mets.xml", gdzfile, false);
            }

            Helper.setMeldung(null, process.getTitle() + ": ", "DMS-Export started");
            if (!ConfigCore.getBooleanParameter("exportWithoutTimeLimit")) {
                DmsImportThread agoraThread = new DmsImportThread(process, atsPpnBand);
                agoraThread.start();
                try {
                    /* 30 Sekunden auf den Thread warten, evtl. killen */
                    agoraThread.join(process.getProject().getDmsImportTimeOut().longValue());
                    if (agoraThread.isAlive()) {
                        agoraThread.stopThread();
                    }
                } catch (InterruptedException e) {
                    if (exportDmsTask != null) {
                        exportDmsTask.setException(e);
                    } else {
                        Helper.setFehlerMeldung(process.getTitle() + ": error on export - ", e.getMessage());
                    }
                    logger.error(process.getTitle() + ": error on export", e);
                }
                if (agoraThread.result.length() > 0) {
                    if (exportDmsTask != null) {
                        exportDmsTask
                                .setException(new RuntimeException(process.getTitle() + ": " + agoraThread.result));
                    } else {
                        Helper.setFehlerMeldung(process.getTitle() + ": ", agoraThread.result);
                    }
                } else {
                    if (exportDmsTask != null) {
                        exportDmsTask.setProgress(100);
                    } else {
                        Helper.setMeldung(null, process.getTitle() + ": ", "ExportFinished");
                    }
                    /* Success-Ordner wieder löschen */
                    if (process.getProject().isDmsImportCreateProcessFolder()) {
                        File successFile = new File(
                                process.getProject().getDmsImportSuccessPath() + File.separator + process.getTitle());
                        fileService.delete(successFile.toURI());
                    }
                }
            }
            if (exportDmsTask != null) {
                exportDmsTask.setProgress(100);
            }
        } else {
            /* ohne Agora-Import die xml-Datei direkt ins Home schreiben */
            if (MetadataFormat.findFileFormatsHelperByName(
                    process.getProject().getFileFormatDmsExport()) == MetadataFormat.METS) {
                writeMetsFile(process, zielVerzeichnis + atsPpnBand + ".xml", gdzfile, false);
            } else {
                gdzfile.write(zielVerzeichnis + atsPpnBand + ".xml");
            }

            Helper.setMeldung(null, process.getTitle() + ": ", "ExportFinished");
        }
        return true;
    }

    /**
     * Setter method to pass in a task thread to whom progress and error
     * messages shall be reported.
     *
     * @param task
     *            task implementation
     */
    public void setExportDmsTask(EmptyTask task) {
        this.exportDmsTask = task;
    }

    /**
     * Run through all metadata and children of given docstruct to trim the
     * strings calls itself recursively.
     */
    private void trimAllMetadata(DocStruct inStruct) {
        /* trimm all metadata values */
        if (inStruct.getAllMetadata() != null) {
            for (Metadata md : inStruct.getAllMetadata()) {
                if (md.getValue() != null) {
                    md.setValue(md.getValue().trim());
                }
            }
        }

        /* run through all children of docstruct */
        if (inStruct.getAllChildren() != null) {
            for (DocStruct child : inStruct.getAllChildren()) {
                trimAllMetadata(child);
            }
        }
    }

    /**
     * Download full text.
     *
     * @param process
     *            object
     * @param userHome
     *            File
     * @param atsPpnBand
     *            String
     * @param ordnerEndung
     *            String
     */
    public void fulltextDownload(Process process, File userHome, String atsPpnBand, final String ordnerEndung)
            throws IOException, InterruptedException, SwapException, DAOException {

        // download sources
        File sources = new File(serviceManager.getProcessService().getSourceDirectory(process));
        if (sources.exists() && fileService.list(sources).length > 0) {
            File destination = new File(userHome + File.separator + atsPpnBand + "_src");
            if (!destination.exists()) {
                destination.mkdir();
            }
            File[] files = fileService.listFiles(sources);
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if (exportDmsTask != null) {
                        exportDmsTask.setWorkDetail(files[i].getName());
                    }
                    File meinZiel = new File(destination + File.separator + files[i].getName());
                    fileService.copyFile(files[i], meinZiel);
                }
            }
        }

        File ocr = new File(serviceManager.getProcessService().getOcrDirectory(process));
        if (ocr.exists()) {
            File[] folder = fileService.listFiles(ocr);
            for (File dir : folder) {
                if (dir.isDirectory() && fileService.list(dir).length > 0 && dir.getName().contains("_")) {
                    String suffix = dir.getName().substring(dir.getName().lastIndexOf("_"));
                    File destination = new File(userHome + File.separator + atsPpnBand + suffix);
                    if (!destination.exists()) {
                        destination.mkdir();
                    }
                    File[] files = fileService.listFiles(dir);
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isFile()) {
                            if (exportDmsTask != null) {
                                exportDmsTask.setWorkDetail(files[i].getName());
                            }
                            File target = new File(destination + File.separator + files[i].getName());
                            fileService.copyFile(files[i], target);
                        }
                    }
                }
            }
        }
        if (exportDmsTask != null) {
            exportDmsTask.setWorkDetail(null);
        }
    }

    /**
     * Download image.
     *
     * @param process
     *            object
     * @param userHome
     *            File
     * @param atsPpnBand
     *            String
     * @param ordnerEndung
     *            String
     */
    public void imageDownload(Process process, File userHome, String atsPpnBand, final String ordnerEndung)
            throws IOException, InterruptedException, SwapException, DAOException {

        /*
         * dann den Ausgangspfad ermitteln
         */
        File tifOrdner = new File(serviceManager.getProcessService().getImagesTifDirectory(true, process));

        /*
         * jetzt die Ausgangsordner in die Zielordner kopieren
         */
        if (tifOrdner.exists() && fileService.list(tifOrdner).length > 0) {
            File zielTif = new File(userHome + File.separator + atsPpnBand + ordnerEndung);

            /* bei Agora-Import einfach den Ordner anlegen */
            if (process.getProject().isUseDmsImport()) {
                if (!zielTif.exists()) {
                    zielTif.mkdir();
                }
            } else {
                /*
                 * wenn kein Agora-Import, dann den Ordner mit
                 * Benutzerberechtigung neu anlegen
                 */
                User myUser = (User) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
                try {
                    fileService.createDirectoryForUser(zielTif.getAbsolutePath(), myUser.getLogin());
                } catch (Exception e) {
                    if (exportDmsTask != null) {
                        exportDmsTask.setException(e);
                    } else {
                        Helper.setFehlerMeldung("Export canceled, error", "could not create destination directory");
                    }
                    logger.error("could not create destination directory", e);
                    if (e instanceof IOException) {
                        throw (IOException) e;
                    } else if (e instanceof InterruptedException) {
                        throw (InterruptedException) e;
                    } else if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    } else {
                        throw new UndeclaredThrowableException(e);
                    }
                }
            }

            /* jetzt den eigentlichen Kopiervorgang */

            File[] dateien = fileService.listFiles(Helper.dataFilter, tifOrdner);
            for (int i = 0; i < dateien.length; i++) {
                if (exportDmsTask != null) {
                    exportDmsTask.setWorkDetail(dateien[i].getName());
                }
                File meinZiel = new File(zielTif + File.separator + dateien[i].getName());
                fileService.copyFile(dateien[i], meinZiel);
                if (exportDmsTask != null) {
                    exportDmsTask.setProgress((int) ((i + 1) * 98d / dateien.length + 1));
                    if (exportDmsTask.isInterrupted()) {
                        throw new InterruptedException();
                    }
                }
            }
            if (exportDmsTask != null) {
                exportDmsTask.setWorkDetail(null);
            }
        }
    }

    /**
     * Starts copying all directories configured in kitodo_config.properties
     * parameter "processDirs" to export folder.
     *
     * @param process
     *            object
     * @param zielVerzeichnis
     *            the destination directory
     *
     */
    private void directoryDownload(Process process, String zielVerzeichnis)
            throws SwapException, DAOException, IOException, InterruptedException {

        String[] processDirs = ConfigCore.getStringArrayParameter("processDirs");

        for (String processDir : processDirs) {
            File srcDir = new File(
                    FilenameUtils.concat(serviceManager.getProcessService().getProcessDataDirectory(process),
                            processDir.replace("(processtitle)", process.getTitle())));
            File dstDir = new File(
                    FilenameUtils.concat(zielVerzeichnis, processDir.replace("(processtitle)", process.getTitle())));

            if (srcDir.isDirectory()) {
                fileService.copyDir(srcDir, dstDir);
            }
        }
    }
}
