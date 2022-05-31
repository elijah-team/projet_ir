CHAVAS Nathan 

MOUDJEB Mohamed

2APPSN





# Rapport projet Intergiciels



## Sommaire



1. Présentation du projet
2. Linda en mode Centralisé
3. Linda en mode Client/Serveur
4. Persistance 
5. Tolérance aux fautes
6. Tests 



## Présentation du projet



Ce rapport présentera le projet réalisé dans le cadre du cours d'Intergiciels suivi à l'ENSEEIHT au cours de l'année 2021-2022. Les différents points qui vont suivre feront état de notre vision quant aux différents choix d'implémentations ainsi que les tests menés afin d'assurer autant que possible la stabilité du programme. 

Tout d'abord, le projet a été réalisé sur GitHub ce qui permet au lecteur de ce rapport de se référer à notre dépôt public (https://github.com/Akodix/projet_ir) afin de consulter notre code.

Nous avons compartimenté notre programme en différentes branches au fur et à mesure de notre avancement comme suit : 

- `main` : Branche contenant la première étape c'est à dire la version centralisée.
- `serveur_client`: Branche ajoutant le mode Client/Serveur
- `backup`: Branche ajoutant la fonction de serveur de secours 

## Linda en mode Centralisé 



La première étape du projet consistait à avoir un fonctionnement centralisé permettant d'ajouter ou bien de lire/récupérer des Tuples au sein d'un espace partagé. Le fait que l'espace soit partagé implique la nécessité d'organiser l'accès à la ressource pour éviter tout conflit. En effet il doit être possible pour plusieurs threads de se mettre en attente de lecture ou de récupération d'un même motif. Il est également nécessaire que les threads demandeurs de tuples se mettent en état d'attente sans que celle-ci soit active sous peine de faire s'effondrer le système en cas de nombreuses demandes. Nous avons donc comme attribut de classe deux listes d'attente. Ces listes d'attentes contiennent des évènements faisant le lien entre le motif de tuple attendu et le callback qui permettra ensuite de "réveiller" le demandeur. Voici les deux listes : 

-  `readerEventList`: Liste les évènements en attente de lecture de tuple.
- `takerEventList`:  Liste les évènements en attente de récupération de tuple.

Nous répondons à la problématique de l'attente passive via l'attribut `sem` de notre classe de callback personnalisé `Tuplecallback`. Il s'agit d'un sémaphore initialisé à 0 ce qui permettra au thread de se mettre en attente passive dès le premier appel de notre méthode d'attente. Notre classe de callback comprend également un attribut Tuple qui sert ici à optimiser notre fonctionnement en retournant directement le tuple dès l'instant du match de motif. 

L'attribut suivant est la liste de tuples `sharedSpace `. C'est notre espace partagé contenant les Tuples voulus par les demandeurs. Il est absolument nécessaire que les trois listes soient en accès exclusif afin d'éviter tout conflit. Nous avons décidé d'utiliser des blocs **synchronized** englobant les instructions de manipulations de ces listes afin de s'assurer qu'à tout moment il n'y a qu'au plus un seul thread en action.

Pour ce qui concerne nos méthodes publiques `read`et `take`, elles sont élémentaires. Elles se contentent de créer un callback pour le thread et d'appeler la méthode `eventRegister` en renseignant le mode, le timing et la combinaison tuple et callback (qui servira à créer l'évènement avant de l'enregistrer) avant de se mettre en attente passive.

C'est donc la méthode `eventRegister` qui traitera toutes les requêtes en fonction du mode et du timing. Dans le cas d'un timing futur, la méthode inscrit directement l'évènement dans la liste d'attente en fonction du mode. Dans le cas d'un timing immédiat, la méthode cherche le motif et le renvoie en cas de match (avec l'astuce de l'attribut tuple de notre callback personnalisé) . Si le tuple n'est pas trouvé on ajoute l'évènement dans la bonne liste d'attente.

Dès lors qu'un tuple est ajouté grâce à la méthode `write` on utilise nos méthodes `notifyReaders`et `notifyTaker` pour réveiller successivement les lecteurs et récupérateurs (l'ordre est important pour éviter que le tuple ne soit retiré avant que les lecteurs ne l'aient consulté). Si le tuple n'a réveillé aucun récupérateur on pourra donc l'ajouter à notre `sharedSpace`

## Linda en mode Client/Serveur 

Concernant cette implémentation, nous avons opté pour les RMI. Les nouvelles classes se trouvent dans le répertoire "server". Nous avons donc ajouté la combinaison de l'interface `LindaRMI` qui extends `Remote` et de son implémentation `LindaRMIImpl` qui extends `UnicastRemoteObject` en implémentant bien sûr `LindaRMI`. L'implémentation ne contient d'un seul attribut de type `Linda` et qui sera ensuite construit comme un `centralizedLinda`. L'ensemble des méthodes consiste simplement à appeler les méthodes déjà présentées dans la classe `centralizedLinda`. Étant donné que les clients et le serveur interagissent de manière distante il nous est maintenant nécessaire de transformer nos callbacks en des objets accessibles à distance. On utilise encore une fois le fonctionnement RMI avec une interface `Remote`et une implémentation `UnicastRemoteObject` qui a pour attribut un callback classique.   

Pour illustrer notre fonctionnement, prenons l'exemple d'un appel de méthode `eventRegister` depuis un client distant. La méthode appelée est celle se trouvant dans notre classe `LindaClient`:

```java
public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
    new Thread(() -> {
        try {
            CallbackRMI remoteCallback = new CallbackRMIImpl(callback);
            this.linda.eventRegister(mode, timing, template, remoteCallback);
        } catch (RemoteException e) {
            System.err.println("Erreur d'execution : " + e);
        }
    }).start();
}
```

On crée un `remoteCallback` qui se construit avec comme paramètre le callback classique de l'appelant. On appelle ensuite la méthode se trouvant cette fois-ci dans notre classe `LindaRMIImpl` : 

```java
public void eventRegister(eventMode mode, eventTiming timing, Tuple template, CallbackRMI callback)
    throws RemoteException {
    Callback localcallback = t -> {
        try {
            callback.call(t);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    };
    this.linda.eventRegister(mode, timing, template, localcallback);
}
```

L'ultime appel d'eventRegister qui est codé dans notre `CentralizedLinda`utilisera bien comme paramètre un callback standard. 

Au final, les implémentations que nous avons crées ne servent qu'à donner un caractère distant aux méthodes que nous avons déjà codées dans notre fonctionnement centralisé.  



## Persistance 

Pour assurer la persistance de l'information au cours des sessions, nous avons implémenté deux autres méthodes : l'une pour charger la sauvegarde et l'autre pour réaliser la sauvegarde. Le chemin de sauvegarde est donné en paramètre au lancement du serveur. On importe `Timer`et `TimerTask` afin d'appeler notre méthode `save` toutes les 10 secondes et on réalise une sauvegarde de sécurité en cas de du fin du processus (dû à un signal d'arrêt ou à la méthode `exit`). On réalise également une sauvegarde à chaque ajout/suppression de tuple (peut s'avérer lourd). 

Comme le reste, nos deux méthodes sont codées dans `CentralizedLinda`. On sauvegarde notre objet `sharedSpace` dans le fichier binaire se trouvant dans l'attribut `filePath`. Quant au chargement, on recrée une liste de tuple à partir de l'objet sauvegardé dans le fichier binaire et on ajoute tous les tuples à notre attribut. 

```java
public void save() {
        try {
            FileOutputStream file_output = new FileOutputStream(filePath);
            ObjectOutputStream object_output = new ObjectOutputStream(file_output);
            object_output.writeObject(this.sharedSpace);
            System.out.println("Mémoire partagée sauvegardée");
            object_output.close();
            file_output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

public void load() {
    if (!Files.exists(Paths.get(filePath)))
        throw new IOError(new RuntimeException("Le fichier spécifié est introuvable"));
    try {
        FileInputStream file_input = new FileInputStream(filePath);
        ObjectInputStream object_input = new ObjectInputStream(file_input);
        List<Tuple> readCase = (List<Tuple>) object_input.readObject();
        this.sharedSpace.addAll(readCase);
        object_input.close();
        file_input.close();
    } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
    }
}
```



## Tolérance aux fautes

#### Côté serveur

Cette dernière partie consiste à offrir une solution dynamique dans le cas où le serveur principal ne fonctionnerait plus. À partir de maintenant x session(s) peuvent vouloir se déclarer comme serveur. On a donc créé une méthode `declareServer`sur notre classe d'implémentation de serveur Linda. Le serveur va essayer d'exécuter un bind. Deux scénarios peuvent se produire : 

1. Le bind fonctionne correctement ce qui veut dire que le serveur courant est le premier à se déclarer comme serveur, il aura donc le statut de serveur principal. On spécifie que l'on est bien serveur principal en assignant `false` à l'attribut booléen `isBackup`.
2. Le bind échoue car il existe déjà un serveur principal (AlreadyBoundException levée). On stocke dans un attribut nommé `backup` l'objet correspondant au serveur principal (grâce à un lookup sur l'URI) et on assigne `True` au booléen `isBackup`. On appelle ensuite sur l'objet stocké dans `backup` (c'est à dire le serveur princiapl) la méthode `registerBackup` en donnant l'instance courante comme paramètre. Le serveur principal stockera ainsi dans son attribut `backup`l'objet correspond à son serveur de secours. On oublie pas d'écrire tous les Tuples du serveur principal dans le `sharedSpace` du serveur de secours (grâce à la méthode `write`). Pour finir le serveur de secours exécutera dans un thread la méthode `pollPrimary` qui vérifiera toutes les deux secondes si le serveur principal est toujours actif. Dans le cas où cela ne serait plus le cas, on appelle la méthode `becomePrimary`qui va rebind, remettre à null l'attribut `backup` et spécifier `isBackup` à `false`. Le serveur secondaire sera bien le nouveau serveur principal et sera prêt à enregistrer tout nouveau serveur comme serveur de secours. Cette implémentation implique que le dernier serveur de secours sera celui qui sera désigné comme serveur principal en cas de chute de celui-ci.

#### Côté client

À la construction du client on appelle une méthode pour lookup le serveur. On crée également une méthode qui permet de relancer un lookup pour contacter le serveur de secours quand le serveur principal tombera. Cette méthode sera donc appelée en cas de  `RemoteException` dans les méthodes appelant les méthodes de l'objet Linda distant. On appellera ensuite récursivement la méthode ayant levée l'erreur pour ne pas perdre l'action ayant révélée le dysfonctionnement du serveur principal. 



## Tests



Pour mesurer l'avancement du projet et le bon fonctionnement de nos ajouts nous avons utilisé la classe `WhiteBoard `. Nous avons également utilisé l'application web fournit au cours des séances en salle pour exécuter les tests sur nos classes. Pour compléter ces tests nous avons ajouté des tests (consultables depuis projet_ir/linda/test).
