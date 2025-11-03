package com.example.authdemo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import com.example.authdemo.model.UserFile;
import com.example.authdemo.repository.UserFileRepository;


@Service
public class UserFileService {
	private final UserFileRepository userFileRepository;
	//private Stack<String> projectList = new Stack<>();

	public UserFileService(UserFileRepository userFileRepository) {
		super();
		this.userFileRepository = userFileRepository;
	}
//	public void addProject(String project) {
//		projectList.add(project);
//	}
	public List<String> getProjects(){		
		return userFileRepository.findDistinctProjects();
	}
	
	public List<String> getDepartments(){		
		return userFileRepository.findDistinctDepartments();
	}
	
	public List<String> getProjectByDepartment(String department){		
		return userFileRepository.findDistinctProjectsByDepartment(department);
	}
    public boolean createUserFile(String fileName,String path,String project,String department) {
        UserFile userFile = new UserFile();
        userFile.setFilename(fileName);
        userFile.setPath(path);
        userFile.setProject(project);
        userFile.setDepartment(department);

        return saveUserFile(userFile);
    }
    
    public boolean saveUserFile(UserFile userFile) {
    	try {
        	userFileRepository.save(userFile);
        	return true;
    	}
    	catch(DataAccessException e) {
    		System.err.println("Database error: " + e.getMessage());
    		return false;
    	}
    }
    
    public Optional<UserFile> findFileByPath(String path) {
    	try {
        	Optional<UserFile> userFile = userFileRepository.findByPath(path);
        	return userFile;
    	}
    	catch(DataAccessException e) {
    		System.err.println("Database error: " + e.getMessage());
    		return null;
    	}
    	
    }
    
    public Optional<UserFile> findFileByFilename(String filename) {
    	try {
        	Optional<UserFile> userFile = userFileRepository.findByFilename(filename);
        	return userFile;
    	}
    	catch(DataAccessException e) {
    		System.err.println("Database error: " + e.getMessage());
    		return null;
    	}
    	
    }
 
    public List<UserFile> getAllFiles() {
        try {
            return userFileRepository.findAll();
        } catch (DataAccessException e) {
            System.err.println(" Database error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

	
	
}
