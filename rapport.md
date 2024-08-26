CHAVAS Nathan 

MOUDJEB Mohamed

2APPSN





- Project report on the Inter-Regulation



- Summary



1. Presentation of the project
2. Linda in Centralized Mode
3. 3. Linda in Customer/Server Mode
4. Persistence 
5. Tolerance to faults
6. Tests 



- Project presentation



This report will present the project carried out as part of the Inter-Information course followed at ENSEEIHT in the year 2021-2022. The various points that will follow will reflect our vision as to the different choices of implementations as well as the tests carried out in order to ensure as far as possible the stability of the programme. 

First of all, the project was carried out on GitHub, which allows the reader of this report to refer to our public repository (https://github.com/Akodix/project-ir) in order to consult our code.

We have compartmentalized our programme into different branches as we progress as we progress as we progress: 

- "Norso: Branch containing the first step, i.e. the centralized version.
- Customer: Branch adding the Client/Server Mode
- Backup: Branch adding the backup server function 

Linda in Centralized Mode 



The first step in the project was to have a centralized operation to add or read/recover Tuples within a shared space. The fact that space is shared implies the need to organize access to the resource in order to avoid any conflict. Indeed, it must be possible for several threads to wait for reading or retrieving the same pattern. It is also necessary for the threads requesting to tuple to put themselves in a state of waiting without the latter being active, otherwise the system will be collapsing in the event of numerous requests. So we have two waiting lists as class attribute. These waiting lists contain events linking the expected tuple motif to the callback which will then make it possible to "awaken" the applicant. Here are the two lists: 

List events awaiting tuple reading.
- Lists events awaiting tuple recovery.

We respond to the problem of passive waiting via the attribute of our custom callback class 'Tuplecallback'. This is a semaphore initialized at 0 which will allow the thread to pass on passive hold on the first call of our waiting method. Our callback class also includes a Tuple attribute that serves here to optimize our operation by turning the tuple directly back from the moment of the pattern match. 

The next attribute is the list of tuples 'sharedSpace'. This is our shared space containing the Tuples wanted by the applicants. It is absolutely necessary that the three lists be in exclusive access in order to avoid any conflict. We have decided to use 'synchronized' blocks encompassing the instructions for manipulating these lists in order to ensure that at any time there is at most one thread in action.

As far as our public methods are concerned, they are elementary. They just create a callback for the thread and call the 'eventRegister' method by filling in the tuple and callback mode, timing and combination (which will be used to create the event before recording it) before taking on passive waiting.

It is therefore the eventRegister method that will process all queries according to mode and timing. In the case of future timing, the method directly places the event in the waiting list according to the mode. In the case of immediate timing, the method searches for the pattern and returns it in case of a match (with the trick of the tuple attribute of our personalized callback). If the tuple is not found, we add the event to the right waiting list.

As soon as a tuple is added using the 'write' method, we use our methods 'notifyReaders' and 'notifyTaker' to successively wake up the readers and recuperators (the order is important to prevent the tuple from being removed before the readers have consulted it). If the tuple hasn't woke up any recuperators, we can add it to our 'sharedSpace'.

Linda in Customer/Warter Mode 

With regard to this implementation, we have opted for the RMIs. The new classes are in the "server" directory. So we added the combination of the interface "LindaRMI" that extends "Remote" and its implementation "LindaRMIImpl" which extended 'UnicastRemoteObject' by of course implementing 'LindaRMI'. The implementation does not contain a single attribute of the type "Linda" and which will then be constructed as a centralizedLinda. The set of methods is simply to call the methods already presented in the centralizedLinda class. Since clients and the server interact remotely, we now need to transform our callbacks into remotely accessible objects. Once again, RMI operation is used with a "Remote" interface and an implementation "UnicastRemoteObject" which has a conventional callback as its attribute.   

To illustrate how we work, let's take the example of a method call from a remote client. The method called is that in our class LindaClient:

zjava
public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback)
    new Thread(() -
        Try it.
            CallbackRMI remoteCallback - new CallbackRMIImpl(callback);
            this.linda.eventRegister(mode, timing, template, remoteCallback);
        - wrestling (RemoteException e)
            System.err.println("Execution error: "e);
        - -
    start();
- -
- -

We create a resetCallback that is built with the conventional caller's callback parameter. Then we call the method this time in our class 'LindaRMIImpl': 

zjava
public void eventRegister(eventMode mode, eventTiming timing, Tuple template, CallbackRMI callback)
    Rotacks RemoteException
    Callback localcallback - t -
        Try it.
            callback.call(t);
        - wrestling (RemoteException e)
            e.printStackTrace();
        - -
    -;
    this.linda.eventRegister(mode, timing, template, localcallback);
- -
- -

The ultimate e-saleRegister call that is coded in our CentralizedLinda will actually use a standard callback as a parameter. 

Ultimately, the implementations that we have created serve only to give a distant character to the methods that we have already encoded in our centralized operation.  



- Persistence 

To ensure the persistence of information during sessions, we implemented two other methods: one to load backup and the other to perform the backup. The backup path is given as a parameter when the server is launched. We import "Timer" and "TimerTask" in order to call our method every 10 seconds and we perform a security backup in case of the end of the process (due to a stop signal or the "exit" method). A backup is also performed at each addition/deletion of tuple (may be heavy). 

Like the rest, our two methods are coded in CentralizedLinda. We save our sharedSpace object to the binary file in the attribute of "filePath". As for the loading, we recreate a list of tuple from the object saved in the binary file and we add all the tuples to our attribute. 

zjava
public void save()
        Try it.
            FileOutputStream file-output - new FileOutputStream(filePath);
            ObjectOutputStream object-output - new ObjectOutputStream (file-output);
            object-output.writeObject(this.sharedSpace);
            System.out.println("Shared memory saved");
            object-output.close();
            file-- output.close();
        Wrestling (IOException e)
            e.printStackTrace();
        - -
    - -

public void load()
    if (Files.exists(Paths.get(filePath(filePath)))
        throw new IOError (new RuntimeException("The specified file cannot be found"));
    Try it.
        FileInputStream file-input - new FileInputStream(filePath);
        ObjectInputStream object-input - new ObjectInputStream (file-input);
        List-- Tuple-- readCase-- (List-- Tuple--) object-- input.readObject();
        this.sharedSpace.addAll(readCase);
        object-- input.close();
        file-- input.close();
    - wrestling (IOException - ClassNotFoundException e)
        e.printStackTrace();
    - -
- -
- -



- Tolerance to faults

Server side

The latter part consists in offering a dynamic solution in the case where the main server no longer functions. From now on x session(s) may wish to declare itself as a server. So we created a method "declareServer" on our Linda server implementation class. The server will try to run a binder. Two scenarios may occur: 

1. The bind operates correctly, which means that the current server is the first to declare itself as a server, so it will have the status of a main server. It is specified that one is a good main server by assigning "false" to the Boolean attribute 'isBackup'.
2. The bind fails because there is already a main server (AlreadyBoundException lifted). The object corresponding to the main server (through a lookup on the URI) is stored in an attribute named "backup" and the Boolean is assigned to the Boolean 'isBackup'. The object stored in backup (i.e. the princiapl server) is then called the recoristerBackup method by giving the current instance as a parameter. The main server will thus store in its attribute backup the object corresponds to its backup server. We don't forget to write all the Tuples of the main server in the "sharedSpace" of the backup server (through the "write" method. Finally, the backup server will execute in a thread the method "pollPrimary" which will check every two seconds whether the main server is still active. In the event that this is no longer the case, the method is called "becomePrimary" which will rejoin, restore to null the attribute "backup" and specify "isBackup" to false. The secondary server will indeed be the new main server and will be ready to register any new server as a backup server. This implementation implies that the last backup server will be the one that will be designated as the main server in the event of a fall of the latter.

- Customer side

At the construction of the client, a method is called a method for looking the server. We also create a method that makes it possible to restart a lookup to contact the backup server when the main server falls. This method will therefore be called in the case of "RemoteException" in the methods calling the methods of the distant Linda object. The method that has lifted the error will then recursively so as not to lose the action that revealed the malfunction of the main server. 



Tests



To measure the progress of the project and the proper functioning of our additions, we used the "WhiteBoard" class. We also used the web application provided during in-room sessions to run the tests on our classes. To complete these tests we have added tests (available from project/linda/test).