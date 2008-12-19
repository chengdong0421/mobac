package tac.program.interfaces;

public interface DownloadJobListener {
	
	public void jobStarted();

	public void jobFinishedSuccessfully(int bytesDownloaded);

	public void jobFinishedWithError();

}
