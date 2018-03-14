/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.tasks;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.console.tasks.XmlReportAssertions.assertValidAccordingToJenkinsSchema;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.io.StringWriter;
import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.test.TestDescriptorStub;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * @since 1.0
 */
class XmlReportWriterTests {

	private EngineDescriptor engineDescriptor = new EngineDescriptor(UniqueId.forEngine("engine"), "Engine");

	@Test
	void writesTestsuiteElementsWithoutTestcaseElementsWithoutAnyTests() throws Exception {
		TestPlan testPlan = TestPlan.from(singleton(engineDescriptor));

		XmlReportData reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());

		String content = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(content);
		//@formatter:off
		assertThat(content)
			.containsSubsequence(
				"<testsuite name=\"Engine\" tests=\"0\"",
				"</testsuite>")
			.doesNotContain("<testcase");
		//@formatter:on
	}

	@Test
	void writesReportEntry() throws Exception {
		UniqueId uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		TestDescriptorStub testDescriptor = new TestDescriptorStub(uniqueId, "successfulTest");
		engineDescriptor.addChild(testDescriptor);
		TestPlan testPlan = TestPlan.from(singleton(engineDescriptor));

		XmlReportData reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		reportData.addReportEntry(TestIdentifier.from(testDescriptor), ReportEntry.from("myKey", "myValue"));
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId.toString()), successful());

		String content = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(content);
		//@formatter:off
		assertThat(content)
			.containsSubsequence(
				"<system-out>",
				"Report Entry #1 (timestamp: ",
				"- myKey: myValue",
				"</system-out>");
		//@formatter:on
	}

	@Test
	void writesEmptySkippedElementForSkippedTestWithoutReason() throws Exception {
		UniqueId uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "skippedTest"));
		TestPlan testPlan = TestPlan.from(singleton(engineDescriptor));

		XmlReportData reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		reportData.markSkipped(testPlan.getTestIdentifier(uniqueId.toString()), null);

		String content = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(content);
		//@formatter:off
		assertThat(content)
			.containsSubsequence(
				"<testcase name=\"skippedTest\"",
				"<skipped/>",
				"</testcase>");
		//@formatter:on
	}

	@Test
	void writesEmptyErrorElementForFailedTestWithoutCause() throws Exception {
		engineDescriptor = new EngineDescriptor(UniqueId.forEngine("myEngineId"), "Fancy Engine") {
			@Override
			public String getLegacyReportingName() {
				return "myEngine";
			}
		};
		UniqueId uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "some fancy name") {
			@Override
			public String getLegacyReportingName() {
				return "failedTest";
			}
		});
		TestPlan testPlan = TestPlan.from(singleton(engineDescriptor));

		XmlReportData reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId.toString()), failed(null));

		String content = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(content);
		//@formatter:off
		assertThat(content)
			.containsSubsequence(
				"<testcase name=\"failedTest\" classname=\"myEngine\"",
				"<error/>",
				"</testcase>");
		//@formatter:on
	}

	@Test
	void omitsMessageAttributeForFailedTestWithThrowableWithoutMessage() throws Exception {
		UniqueId uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "failedTest"));
		TestPlan testPlan = TestPlan.from(singleton(engineDescriptor));

		XmlReportData reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId.toString()), failed(new NullPointerException()));

		String content = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(content);
		//@formatter:off
		assertThat(content)
			.containsSubsequence(
				"<testcase name=\"failedTest\"",
				"<error type=\"java.lang.NullPointerException\">",
				"</testcase>");
		//@formatter:on
	}

	@Test
	void writesValidXmlEvenIfExceptionMessageContainsCData() throws Exception {
		UniqueId uniqueId = engineDescriptor.getUniqueId().append("test", "test");
		engineDescriptor.addChild(new TestDescriptorStub(uniqueId, "test"));
		TestPlan testPlan = TestPlan.from(singleton(engineDescriptor));

		XmlReportData reportData = new XmlReportData(testPlan, Clock.systemDefaultZone());
		AssertionError assertionError = new AssertionError("<foo><![CDATA[bar]]></foo>");
		reportData.markFinished(testPlan.getTestIdentifier(uniqueId.toString()), failed(assertionError));

		String content = writeXmlReport(testPlan, reportData);

		assertValidAccordingToJenkinsSchema(content);
		//@formatter:off
		assertThat(content)
				.containsSubsequence(
						"<![CDATA[",
						"<foo><![CDATA[bar]]]]><![CDATA[></foo>",
						"]]>")
				.doesNotContain(assertionError.getMessage());
		//@formatter:on
	}

	private String writeXmlReport(TestPlan testPlan, XmlReportData reportData) throws Exception {
		StringWriter out = new StringWriter();
		new XmlReportWriter(reportData).writeXmlReport(getOnlyElement(testPlan.getRoots()), out);
		return out.toString();
	}

}
