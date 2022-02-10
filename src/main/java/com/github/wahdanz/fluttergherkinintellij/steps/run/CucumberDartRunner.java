package com.github.wahdanz.fluttergherkinintellij.steps.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService;
import com.jetbrains.lang.dart.ide.runner.DartRunner;
import com.jetbrains.lang.dart.ide.runner.base.DartRunConfigurationBase;
import com.jetbrains.lang.dart.ide.runner.server.vmService.DartVmServiceDebugProcess;
import com.jetbrains.lang.dart.sdk.DartSdk;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CucumberDartRunner extends GenericProgramRunner {
	private static final Logger LOG = Logger.getInstance(DartRunner.class.getName());

	@NotNull
	@Override
	public String getRunnerId() {
		return "CucumberDartRunner";
	}

	@Override
	public boolean canRun(final @NotNull String executorId, final @NotNull RunProfile profile) {
		boolean canRun = DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) &&
			(profile instanceof CucumberDartRunConfiguration);
		if (canRun) {
      CucumberDartRunConfiguration p = (CucumberDartRunConfiguration)profile;
      canRun = !(CucumberDartRunnerParameters.isFlutterDriverExecutable(p.getRunnerParameters()));
    }

		return canRun;
	}

	@Override
	protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
		final String executorId = env.getExecutor().getId();
		if (!DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)) {
			LOG.error("Unexpected executor id: " + executorId);
			return null;
		}

		try {
			final String dasExecutionContextId;

			final RunProfile runConfig = env.getRunProfile();
			if (state instanceof CucumberDartRunningTestState && runConfig instanceof DartRunConfigurationBase &&
				DartAnalysisServerService.getInstance(env.getProject()).serverReadyForRequest()) {
			  CucumberDartRunnerParameters runParams =  ((CucumberDartRunningTestState)state).myRunnerParameters;
				String path = runParams.getDartFilePath();
        if (path == null) {
          path = runParams.getFilePath();
        }
				assert path != null; // already checked
				dasExecutionContextId = DartAnalysisServerService.getInstance(env.getProject()).execution_createContext(path);
			}
			else {
				dasExecutionContextId = null; // remote debug or can't start DAS
			}

			return doExecuteDartDebug(state, env, dasExecutionContextId);
		}
		catch (RuntimeConfigurationError e) {
			throw new ExecutionException(e);
		}
	}

	protected int getTimeout() {
		return 5000; // Allow 5 seconds to connect to the observatory.
	}

	private RunContentDescriptor doExecuteDartDebug(final @NotNull RunProfileState state,
	                                                final @NotNull ExecutionEnvironment env,
	                                                final @Nullable String dasExecutionContextId) throws RuntimeConfigurationError,
		ExecutionException {
		final DartSdk sdk = DartSdk.getDartSdk(env.getProject());
		assert (sdk != null); // already checked

		final RunProfile runConfiguration = env.getRunProfile();
		final VirtualFile contextFileOrDir;
		VirtualFile currentWorkingDirectory;
		final ExecutionResult executionResult;
		final String debuggingHost;
		final int observatoryPort;

		if (runConfiguration instanceof CucumberDartRunConfiguration && state instanceof CucumberDartRunningTestState) {
      CucumberDartRunnerParameters runParams =  ((CucumberDartRunningTestState)state).myRunnerParameters;
			contextFileOrDir = runParams.getDartFileOrDirectory();

			final String cwd = runParams.computeProcessWorkingDirectory(env.getProject());
			currentWorkingDirectory = LocalFileSystem.getInstance().findFileByPath((cwd));

			executionResult = state.execute(env.getExecutor(), this);
			if (executionResult == null) {
				return null;
			}
		} else {
			LOG.error("Unexpected run configuration: " + runConfiguration.getClass().getName());
			return null;
		}

		FileDocumentManager.getInstance().saveAllDocuments();

		final XDebuggerManager debuggerManager = XDebuggerManager.getInstance(env.getProject());
		final XDebugSession debugSession = debuggerManager.startSession(env, new XDebugProcessStarter() {
			@Override
			@NotNull
			public XDebugProcess start(@NotNull final XDebugSession session) throws ExecutionException {
        final DartUrlResolver dartUrlResolver = getDartUrlResolver(env.getProject(), contextFileOrDir);
        final DartVmServiceDebugProcess.DebugType debugType =  DartVmServiceDebugProcess.DebugType.CLI;
        DartVmServiceDebugProcess debugProcess = new DartVmServiceDebugProcess(session,
          executionResult,
          dartUrlResolver,
          dasExecutionContextId,
          debugType,
          getTimeout(),
          currentWorkingDirectory);
        debugProcess.start();
        return debugProcess;
			}
		});

		return debugSession.getRunContentDescriptor();
	}

	protected DartUrlResolver getDartUrlResolver(@NotNull final Project project, @NotNull final VirtualFile contextFileOrDir) {
		return DartUrlResolver.getInstance(project, contextFileOrDir);
	}
}
