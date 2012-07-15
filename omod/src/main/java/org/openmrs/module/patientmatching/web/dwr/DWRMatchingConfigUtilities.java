/**
 *
 */
package org.openmrs.module.patientmatching.web.dwr;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.ServerContext;
import org.directwebremoting.ServerContextFactory;
import org.directwebremoting.proxy.dwr.Util;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientmatching.*;
import org.openmrs.util.OpenmrsUtil;

import org.regenstrief.linkage.util.MatchingConfig;

import uk.ltd.getahead.dwr.WebContext;
import uk.ltd.getahead.dwr.WebContextFactory;

/**
 * Utility class that will be available to the DWR javascript call from the
 * module web page. All methods in this class must be registered in module
 * config file to make it available as javascript call.
 */
public class DWRMatchingConfigUtilities {

	protected final Log log = LogFactory.getLog(getClass());
	private static ServerContext sctx;
	private static String currentPage;
	private static Map objects;
	private static Boolean processStarted = false;
	private static String previousProcessTime = "0,0";
	private static Long time;
	private static int reset;
	private static int index = 1;
	private static int currentStep;
	private static int size = 0;
	private static String[] selectedStrat;

	/**
	 * Constructor
	 */
	public DWRMatchingConfigUtilities() {
		ServletContext servletContext = org.directwebremoting.WebContextFactory.get().getServletContext();
		sctx = ServerContextFactory.get(servletContext);
		org.directwebremoting.WebContext wctx = org.directwebremoting.WebContextFactory.get();
		currentPage = wctx.getCurrentPage();
	}

	/**
	 * @see MatchingConfigurationUtils#listAvailableBlockingRuns()
	 */
	public List<String> getAllBlockingRuns() {
		return MatchingConfigurationUtils.listAvailableBlockingRuns_db();
	}

	/**
	 * returns a list of [id, name] arrays for all patient matching configurations.
	 */
	public List<Object[]> getAllPatientMatchingConfigurations() {
		// get all configs
		PatientMatchingReportMetadataService service = Context.getService(PatientMatchingReportMetadataService.class);
		List<PatientMatchingConfiguration> configs = service.getMatchingConfigs();
		
		// convert to arrays of [id, name]
		List<Object[]> results = new ArrayList<Object[]>();
		for (PatientMatchingConfiguration config: configs)
			results.add(new Object[]{ config.getConfigurationId(), config.getConfigurationName()});
		
		// send them back
		return results;
	}

	/**
	 * @see MatchingReportUtils#listAvailableReportNamesInDB()
	 */
	public List<String> getAllReports() {
		return MatchingReportUtils.listAvailableReportNamesInDB();
	}

	/**
	 * @see PatientMatchingReportMetadataService#deletePatientMatchingConfiguration(Long) 
	 */
	public void deleteBlockingRun(Long id) {
		log.info("DWRMatchingConfigUtilities: deleting blocking run #" + id);
		PatientMatchingReportMetadataService service = Context.getService(PatientMatchingReportMetadataService.class);
		PatientMatchingConfiguration configuration = service.getPatientMatchingConfiguration(id.intValue());
		if (configuration != null)
			service.deletePatientMatchingConfiguration(configuration);
	}

	/**
	 * @see MatchingReportUtils#resetStep()
	 */
	public void resetStep() {
		log.info("DWRMatchingConfigUtilities: resetting to first step");
		reset = -1;
		currentStep = 0;
		Collection sessions = sctx.getScriptSessionsByPage(currentPage);
		Util pages = new Util(sessions);
		pages.addFunctionCall("reset");
	}

	/**
	 * @see MatchingReportUtils#getStep()
	 */
	public String getStep() {
		boolean timerTaskStarted = MatchingRunData.getInstance().isTimerTaskStarted();
		
		int step = currentStep;
		String activeReverseAjaxEnabled = "true";
		ServletConfig sc = org.directwebremoting.WebContextFactory.get().getServletConfig();
		if (sc.getInitParameter("activeReverseAjaxEnabled") == null || sc.getInitParameter("activeReverseAjaxEnabled").equals("false")) {
			activeReverseAjaxEnabled = "false," + reset + "," + timerTaskStarted;
		}
		if (timerTaskStarted) {
			Collection sessions3 = sctx.getScriptSessionsByPage(currentPage);
			Util pages3 = new Util(sessions3);
			pages3.addFunctionCall("scheduledTaskRunning");
		}
		log.info("DWRMatchingConfigUtilities: returning step " + new Integer(step));
		return (new Integer(step).toString()) + "," + processStarted.toString() + "," + activeReverseAjaxEnabled;
	}

	public List<Object> getPatient(String patientId) {
		List<Object> patientDetails = new ArrayList<Object>();
		int pId = Integer.valueOf(patientId);
		Patient patient = Context.getPatientService().getPatient(pId);
		Set<PatientIdentifier> patientIdentifier = Context.getPatientService().getPatient(pId).getIdentifiers();
		Set<PersonAddress> patientAddress = Context.getPatientService().getPatient(pId).getAddresses();
		patientDetails.add(patient);
		patientDetails.add(patientIdentifier);
		patientDetails.add(patientAddress);

		return patientDetails;
	}

	public List<Long> previousProcessStatus() {
		return MatchingRunData.getInstance().getProTimeList();
	}

	public void selStrategy(String selected) {
		WebContext context = WebContextFactory.get();
		HttpSession session = context.getSession();
		session.removeAttribute("selStrategy");
		session.setAttribute("selStrategy", selected);
	}

	public void getCurrentProcessStatus(int nextStep) {

		time = Calendar.getInstance().getTimeInMillis();
		try {
			switch (nextStep) {
				case 2:
					objects = new HashMap<String, Object>();
					objects = MatchingReportUtils.ReadConfigFile(objects, selectedStrat);
					break;

				case 3:
					objects = MatchingReportUtils.InitScratchTable(objects);
					size = ((List<MatchingConfig>) objects.get("matchingConfigLists")).size();
					break;

				case 4:
					objects = MatchingReportUtils.CreRanSamAnalyzer(objects);
					break;

				case 5:
					objects = MatchingReportUtils.CreAnalFormPairs(objects);
					break;

				case 6:
					objects = MatchingReportUtils.CrePairdataSourAnalyzer(objects);
					break;

				case 7:
					objects = MatchingReportUtils.CreEMAnalyzer(objects);
					break;

				case 8:
					objects = MatchingReportUtils.AnalyzingData(objects);
					break;

				case 9:
					objects = MatchingReportUtils.ScoringData(objects);
					break;

				case 10:
					objects = MatchingReportUtils.CreatingReport(objects);
					break;
			}

		} catch (Exception e) {
			LinkDBConnections.getInstance().releaseLock();
			log.warn("Exception caught during the analysis process", e);
			reset = -1;
		} catch (Throwable t) {
			LinkDBConnections.getInstance().releaseLock();
			log.warn("Throwable object caught during the analysis process", t);
			reset = -1;
		}

		time = Calendar.getInstance().getTimeInMillis() - time;
	}

	/**
	 * @see MatchingReportUtils#doAnalysis()
	 */
	public void doAnalysis(String selectedStrategies) {
		MatchingRunData.getInstance().setFileStrat(selectedStrategies);
		if (!MatchingRunData.getInstance().isTimerTaskStarted()) {
			selectedStrat = selectedStrategies.split(",");
			MatchingRunData.getInstance().setProTimeList(new ArrayList<Long>());
			Collection sessions = sctx.getScriptSessionsByPage(currentPage);
			Util pages = new Util(sessions);
			pages.addFunctionCall("reportProcessStarted");
			reset = 0;
			for (int i = 2; i < 11; i++) {
				currentStep = i;
				processStarted = true;
				getCurrentProcessStatus(i);
				processStarted = false;
				if (reset == -1) {
					Collection sessions1 = sctx.getScriptSessionsByPage(currentPage);
					Util pages1 = new Util(sessions1);
					pages1.addFunctionCall("enableGenReport");
					break;
				}

				if (size > 1 && index > 1 && i >= 4 && i <= 9) {
					time = (time + MatchingRunData.getInstance().getProTimeList().get(i - 2));
					if (i == 9 && size != index) {
						previousProcessTime = "3," + time + "p";
					} else {
						previousProcessTime = i + "," + time;
					}
					MatchingRunData.getInstance().getProTimeList().set((i - 2), time);
				} else {
					if (size > 1 && i == 9) {
						previousProcessTime = "3," + time + "p";
					} else {
						previousProcessTime = i + "," + time;
					}
					MatchingRunData.getInstance().getProTimeList().add(time);
				}

				if (i == 9 && size != index && size != 0) {
					objects.put("matchingConfig", ((List<MatchingConfig>) objects.get("matchingConfigLists")).get(index));
					index++;
					i = 3;
				}

				if (reset != -1) {
					Collection sessions2 = sctx.getScriptSessionsByPage(currentPage);
					Util pages2 = new Util(sessions2);
					pages2.addFunctionCall("updateChecklist", previousProcessTime);

				} else {
					MatchingRunData.getInstance().setProTimeList(null);
					currentStep = 0;
				}
			}

			processStarted = false;
			currentStep = 0;
			index = 1;
			size = 0;
		} else {
			Collection sessions3 = sctx.getScriptSessionsByPage(currentPage);
			Util pages3 = new Util(sessions3);
			pages3.addFunctionCall("scheduledTaskRunning");
		}
	}

	/**
	 * Delete a particular report file from the server using DWR call
	 *
	 * @param filename report file that will be deleted
	 */
	public void deleteReportFile(String filename) {
		log.info("DWRMatchingConfigUtilities: deleting file " + filename);
		String configLocation = MatchingConstants.CONFIG_FOLDER_NAME;
		File configFileFolder = OpenmrsUtil.getDirectoryInApplicationDataDirectory(configLocation);
		File reportFile = new File(configFileFolder, filename);
		log.info("Report file to be deleted: " + reportFile.getAbsolutePath());
		boolean deleted = reportFile.delete();
		if (deleted) {
			log.info("Config file deleted.");
		}
	}

	/**
	 * Accessing report name using DWR call
	 */
	public static void setReportName(String filename) {
		MatchingRunData.getInstance().setRptname(filename);
	}

	/**
	 * Get the report block for a particular page out of the report file using
	 * DWR call.
	 *
	 * @return content of the next page in the report file
	 */
	@SuppressWarnings("unchecked")
	public List<List<String>> getNextPage() {
		WebContext context = WebContextFactory.get();
		HttpSession session = context.getSession();
		String filename = (String) session.getAttribute("reportFilename");
		List<Long> pagePos = (List<Long>) session.getAttribute("reportPagePosition");
		int thisPage = (Integer) session.getAttribute("reportCurrentPage");
		boolean eof = (Boolean) session.getAttribute("isReportEOF");

		List<List<String>> currentContent = new ArrayList<List<String>>();
		// init with error message
		List<String> s = new ArrayList<String>();
		s.add("Unable to get the report data, please retry or re-open the report page");
		currentContent.add(s);

		MatchingReportReader reader = new MatchingReportReader(thisPage,
				eof, pagePos, filename);
		try {
			if (eof) {
				reader.fetchContent(thisPage);
				if (session.getAttribute("endPage") == null) {
					session.setAttribute("endPage", thisPage);
				}
			} else {
				reader.fetchContent(thisPage + 1);
			}
			// only update the value when succeed
			session.setAttribute("reportPagePosition", reader.getPagePos());
			session.setAttribute("reportCurrentPage", reader.getCurrentPage());
			session.setAttribute("isReportEOF", reader.isEof());
			// this will replace error message if the process is done
			currentContent = reader.getCurrentContent();
		} catch (IOException e) {
			log.warn("error fetching content or setting session attributes", e);
		}
		return currentContent;
	}

	/**
	 * Get the report block for a particular page out of the report file using
	 * DWR call.
	 *
	 * @return content of the previous page in the report file
	 */
	@SuppressWarnings("unchecked")
	public List<List<String>> getPrevPage() {
		WebContext context = WebContextFactory.get();
		HttpSession session = context.getSession();
		String filename = (String) session.getAttribute("reportFilename");
		List<Long> pagePos = (List<Long>) session.getAttribute("reportPagePosition");
		int thisPage = (Integer) session.getAttribute("reportCurrentPage");
		// pressing previous button always makes the eof false
		boolean eof = false;

		List<List<String>> currentContent = new ArrayList<List<String>>();
		// init with error message
		List<String> s = new ArrayList<String>();
		s.add("Unable to get the report data, please retry or re-open the report page");
		currentContent.add(s);

		MatchingReportReader reader = new MatchingReportReader(thisPage,
				eof, pagePos, filename);
		try {
			if (thisPage > 1) {
				reader.fetchContent(thisPage - 1);
			} else {
				reader.fetchContent(thisPage);
			}
			// only update the value when succeed
			session.setAttribute("reportPagePosition", reader.getPagePos());
			session.setAttribute("reportCurrentPage", reader.getCurrentPage());
			session.setAttribute("isReportEOF", reader.isEof());
			// this will replace error message if the process is done
			currentContent = reader.getCurrentContent();
		} catch (IOException e) {
			log.warn("error fetching content or setting session attributes", e);
		}
		
		return currentContent;
	}

	public List<List<String>> getStartPage() {
		WebContext context = WebContextFactory.get();
		HttpSession session = context.getSession();

		String filename = (String) session.getAttribute("reportFilename");
		List<Long> pagePos = (List<Long>) session.getAttribute("reportPagePosition");
		int thisPage = 1;
		boolean eof = false;

		List<List<String>> currentContent = new ArrayList<List<String>>();
		// init with error message
		List<String> s = new ArrayList<String>();
		s.add("Unable to get the report data, please retry or re-open the report page");
		currentContent.add(s);

		MatchingReportReader reader = new MatchingReportReader(thisPage, eof, pagePos, filename);

		try {
			reader.fetchContent(thisPage);

			// only update the value when succeed
			session.setAttribute("reportPagePosition", reader.getPagePos());
			session.setAttribute("reportCurrentPage", reader.getCurrentPage());
			session.setAttribute("isReportEOF", reader.isEof());

			// this will replace error message if the process is done
			currentContent = reader.getCurrentContent();
			
		} catch (IOException e) {
			log.warn("error fetching content or setting session attributes", e);
		}
		
		return currentContent;
	}

	public List<List<String>> getEndPage() {
		List<List<String>> currentContent = new ArrayList<List<String>>();

		try {

			WebContext context = WebContextFactory.get();
			HttpSession session = context.getSession();

			String filename = (String) session.getAttribute("reportFilename");
			int thisPage = (Integer) session.getAttribute("reportCurrentPage");
			boolean eof = (Boolean) session.getAttribute("isReportEOF");
			List<Long> pagePos = (List<Long>) session.getAttribute("reportPagePosition");

			if (session.getAttribute("endPage") == null) {
				String configLocation = MatchingConstants.CONFIG_FOLDER_NAME;
				File configFileFolder = OpenmrsUtil.getDirectoryInApplicationDataDirectory(configLocation);
				File reportFile = new File(configFileFolder, filename);

				RandomAccessFile raf = new RandomAccessFile(reportFile, "r");
				double count = -1;
				while (raf.readLine() != null)
					++count;

				int totalPages = (int) Math.ceil(count / MatchingReportReader.REPORT_PAGE_SIZE);
				int counter = 0;
				long offset = pagePos.get(thisPage);
				raf.seek(offset);

				while (raf.readLine() != null) {
					++counter;
					if (counter >= MatchingReportReader.REPORT_PAGE_SIZE) {
						thisPage++;
						counter = 0;
						boolean incl = false;
						try {
							pagePos.get(thisPage);
						} catch (IndexOutOfBoundsException e) {
							incl = true;
						}
						if (incl) {
							pagePos.add(raf.getFilePointer());
						}
					}
				}
				thisPage = totalPages;
				session.setAttribute("endPage", totalPages);
			} else {
				thisPage = (Integer) session.getAttribute("endPage");
			}
			
			// init with error message
			List<String> s = new ArrayList<String>();
			s.add("Unable to get the report data, please retry or re-open the report page");
			currentContent.add(s);

			MatchingReportReader reader = new MatchingReportReader(thisPage, eof, pagePos, filename);

			reader.fetchContent(thisPage);

			session.setAttribute("reportPagePosition", reader.getPagePos());
			session.setAttribute("reportCurrentPage", reader.getCurrentPage());
			session.setAttribute("isReportEOF", reader.isEof());
			
			// this will replace error message if the process is done
			currentContent = reader.getCurrentContent();

		} catch (IOException e) {
			log.info(e.getLocalizedMessage(), e);
		}

		return currentContent;
	}
}