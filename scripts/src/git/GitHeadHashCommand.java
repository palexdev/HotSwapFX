package src.git;

import src.commons.Command;

//@formatter:off
/// Retrieves the HEAD commit short hash
public class GitHeadHashCommand extends Command<String> {
 @Override protected String[] args() {return new String[]{"git", "log", "-1", "--pretty=format:%h"};}
 @Override protected String parse(String output) {return output.trim();}
}
